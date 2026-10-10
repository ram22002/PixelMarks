package com.pixelmarks.core.pcserver

import com.google.gson.Gson
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.pcserver.model.BookmarkDto
import com.pixelmarks.core.pcserver.model.CollectionDto
import com.pixelmarks.core.pcserver.model.ErrorResponse
import com.pixelmarks.core.pcserver.model.PairRequest
import com.pixelmarks.core.pcserver.model.PairResponse
import com.pixelmarks.core.pcserver.model.PairingDecision
import com.pixelmarks.core.pcserver.model.PcServerState
import com.pixelmarks.core.pcserver.model.ServerStatusResponse
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.CollectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Embedded local HTTP server providing authenticated read-only access to public bookmarks.
 * Never exposes the server automatically or to the public internet.
 */
class LocalServerManager(
    private val bookmarkRepository: BookmarkRepository,
    private val collectionRepository: CollectionRepository,
    private val pairingManager: PairingManager,
    private val sessionManager: DeviceSessionManager,
    private val gson: Gson = Gson()
) {

    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val threadPool = Executors.newFixedThreadPool(8)

    private val _serverState = MutableStateFlow<PcServerState>(PcServerState.Stopped)
    val serverState: StateFlow<PcServerState> = _serverState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private val activeSockets = ConcurrentHashMap.newKeySet<Socket>()

    @Volatile
    private var isRunning = false

    /**
     * Starts the local server on the specified port (default 8080).
     */
    @Synchronized
    fun startServer(port: Int = DEFAULT_PORT): Boolean {
        if (isRunning) return true

        _serverState.value = PcServerState.Starting

        val localIp = LocalNetworkUtil.getLocalIpAddress() ?: "127.0.0.1"

        try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
            serverSocket = socket
            isRunning = true

            _serverState.value = PcServerState.Running(
                ipAddress = localIp,
                port = socket.localPort
            )

            serverScope.launch {
                listenForClients(socket, localIp)
            }
            return true
        } catch (e: Exception) {
            _serverState.value = PcServerState.Error(
                message = "Failed to start local server on port $port: ${e.message}",
                cause = e
            )
            stopServer()
            return false
        }
    }

    /**
     * Stops the local server, closes all active connections, and invalidates temporary pairing codes.
     */
    @Synchronized
    fun stopServer() {
        isRunning = false
        pairingManager.cancelActiveCode()

        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null

        activeSockets.forEach { socket ->
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
        activeSockets.clear()

        _serverState.value = PcServerState.Stopped
    }

    private suspend fun listenForClients(server: ServerSocket, localIp: String) = withContext(Dispatchers.IO) {
        while (isRunning && !server.isClosed) {
            try {
                val clientSocket = server.accept()
                activeSockets.add(clientSocket)

                threadPool.execute {
                    handleClientConnection(clientSocket, localIp)
                }
            } catch (e: Exception) {
                if (!isRunning || server.isClosed) break
            }
        }
    }

    private fun handleClientConnection(socket: Socket, localIp: String) {
        try {
            socket.soTimeout = 15_000 // 15 seconds read timeout

            val remoteAddress = socket.inetAddress
            if (!isAllowedClientIp(remoteAddress)) {
                sendErrorResponse(socket.getOutputStream(), 403, "Forbidden", "Only local network connections are permitted.")
                return
            }

            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 3) {
                sendErrorResponse(socket.getOutputStream(), 400, "Bad Request", "Malformed HTTP request.")
                return
            }

            val method = parts[0].uppercase()
            val fullPath = parts[1]
            val path = if (fullPath.contains("?")) fullPath.substringBefore("?") else fullPath

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            var headerCount = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                headerCount++
                if (headerCount > 100) {
                    sendErrorResponse(socket.getOutputStream(), 431, "Request Header Fields Too Large", "Header count exceeded limit.")
                    return
                }
                val colonIndex = line!!.indexOf(':')
                if (colonIndex > 0) {
                    val key = line!!.substring(0, colonIndex).trim().lowercase()
                    val value = line!!.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
            }

            // Cross-Origin / CSRF checks
            val secFetchSite = headers["sec-fetch-site"]
            if (secFetchSite == "cross-site") {
                sendErrorResponse(socket.getOutputStream(), 403, "Forbidden", "Cross-site requests are rejected.")
                return
            }

            val origin = headers["origin"]
            if (origin != null && !isOriginAllowed(origin, localIp, socket.localPort)) {
                sendErrorResponse(socket.getOutputStream(), 403, "Forbidden", "Disallowed Origin.")
                return
            }

            // Handle OPTIONS preflight
            if (method == "OPTIONS") {
                sendCorsOptionsResponse(socket.getOutputStream())
                return
            }

            // Read body if Content-Length specified
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            if (contentLength > MAX_PAYLOAD_BYTES) {
                sendErrorResponse(socket.getOutputStream(), 413, "Payload Too Large", "Request payload exceeds 64KB limit.")
                return
            }

            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = reader.read(chars, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                String(chars, 0, readTotal)
            } else ""

            // Dispatch route synchronously before closing socket
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                dispatchRoute(
                    socket = socket,
                    method = method,
                    path = path,
                    headers = headers,
                    body = body,
                    remoteIp = remoteAddress.hostAddress ?: "Unknown"
                )
            }

        } catch (_: SocketTimeoutException) {
            // Connection timed out
        } catch (_: Exception) {
            // Socket or I/O error
        } finally {
            activeSockets.remove(socket)
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun dispatchRoute(
        socket: Socket,
        method: String,
        path: String,
        headers: StringMap,
        body: String,
        remoteIp: String
    ) {
        val output = socket.getOutputStream()

        when {
            // Landing Web Client
            (method == "GET" && (path == "/" || path == "/index.html")) -> {
                sendHtmlResponse(output, PC_CLIENT_HTML)
            }

            // Public Handshake
            (method == "GET" && path == "/api/v1/status") -> {
                val response = ServerStatusResponse()
                sendJsonResponse(output, 200, "OK", gson.toJson(response))
            }

            // Initiate Pairing
            (method == "POST" && path == "/api/v1/pair") -> {
                handlePairingRequest(output, body, remoteIp, headers["user-agent"] ?: "")
            }

            // Bookmarks Listing (Protected)
            (method == "GET" && path == "/api/v1/bookmarks") -> {
                handleGetBookmarks(output, headers)
            }

            // Single Bookmark Detail (Protected)
            (method == "GET" && path.startsWith("/api/v1/bookmarks/")) -> {
                val idStr = path.removePrefix("/api/v1/bookmarks/")
                val bookmarkId = idStr.toLongOrNull()
                if (bookmarkId == null) {
                    sendErrorResponse(output, 400, "Bad Request", "Invalid bookmark ID.")
                } else {
                    handleGetBookmarkDetail(output, headers, bookmarkId)
                }
            }

            // Collections Listing (Protected)
            (method == "GET" && path == "/api/v1/collections") -> {
                handleGetCollections(output, headers)
            }

            // Revoke Current Session (Protected)
            (method == "POST" && path == "/api/v1/revoke") -> {
                handleRevokeSession(output, headers)
            }

            else -> {
                sendErrorResponse(output, 404, "Not Found", "Endpoint not found: $method $path")
            }
        }
    }

    private suspend fun handlePairingRequest(
        output: OutputStream,
        body: String,
        remoteIp: String,
        userAgent: String
    ) {
        val pairRequest = try {
            gson.fromJson(body, PairRequest::class.java)
        } catch (_: Exception) {
            null
        }

        if (pairRequest == null || pairRequest.pairingCode.isBlank()) {
            sendErrorResponse(output, 400, "Bad Request", "Missing or malformed pairing credentials.")
            return
        }

        val initiateResult = pairingManager.initiatePairing(
            inputCode = pairRequest.pairingCode,
            deviceName = pairRequest.deviceName,
            clientIp = remoteIp,
            userAgent = userAgent
        )

        when (initiateResult) {
            is PairingManager.InitiateResult.Error -> {
                val status = if (initiateResult.isLocked) 429 else 400
                val statusText = if (initiateResult.isLocked) "Too Many Requests" else "Bad Request"
                val response = PairResponse(
                    success = false,
                    status = if (initiateResult.isLocked) "LOCKED" else "INVALID_CODE",
                    message = initiateResult.message
                )
                sendJsonResponse(output, status, statusText, gson.toJson(response))
            }
            is PairingManager.InitiateResult.AwaitingApproval -> {
                // Suspends until user approves/denies on the Android device
                val decision = pairingManager.awaitDecision(initiateResult.requestId)
                when (decision) {
                    is PairingDecision.Approved -> {
                        val (_, token) = sessionManager.createDeviceSession(pairRequest.deviceName)
                        val response = PairResponse(
                            success = true,
                            token = token,
                            status = "APPROVED",
                            message = "Successfully paired with PixelMarks on Android."
                        )
                        sendJsonResponse(output, 200, "OK", gson.toJson(response))
                    }
                    is PairingDecision.Denied -> {
                        val response = PairResponse(
                            success = false,
                            status = "DENIED",
                            message = "Pairing request was declined on the Android device."
                        )
                        sendJsonResponse(output, 403, "Forbidden", gson.toJson(response))
                    }
                    is PairingDecision.TimedOut -> {
                        val response = PairResponse(
                            success = false,
                            status = "TIMED_OUT",
                            message = "Pairing request timed out. Please try again."
                        )
                        sendJsonResponse(output, 408, "Request Timeout", gson.toJson(response))
                    }
                }
            }
        }
    }

    private suspend fun handleGetBookmarks(output: OutputStream, headers: StringMap) {
        val device = authenticateRequest(headers)
        if (device == null) {
            sendUnauthorizedResponse(output)
            return
        }

        val collectionsMap = collectionRepository.getAllByPrivacy(isPrivate = false)
            .associateBy { it.id }

        // Strictly public bookmarks only (isPrivate = false)
        val publicBookmarks = bookmarkRepository.getAllByPrivacy(isPrivate = false)
            .filter { !it.isTrashed }
            .map { bookmark ->
                val col = bookmark.collectionId?.let { collectionsMap[it] }
                BookmarkDto(
                    id = bookmark.id,
                    url = bookmark.url,
                    title = bookmark.title,
                    description = bookmark.description,
                    collectionId = bookmark.collectionId,
                    collectionName = col?.name,
                    isFavorite = bookmark.isFavorite,
                    isPinned = bookmark.isPinned,
                    isArchived = bookmark.isArchived,
                    visitCount = bookmark.visitCount,
                    createdAt = bookmark.createdAt,
                    updatedAt = bookmark.updatedAt
                )
            }

        sendJsonResponse(output, 200, "OK", gson.toJson(publicBookmarks))
    }

    private suspend fun handleGetBookmarkDetail(output: OutputStream, headers: StringMap, bookmarkId: Long) {
        val device = authenticateRequest(headers)
        if (device == null) {
            sendUnauthorizedResponse(output)
            return
        }

        val bookmark = bookmarkRepository.getById(bookmarkId)
        // Privacy check: NEVER leak private or trashed bookmarks
        if (bookmark == null || bookmark.isPrivate || bookmark.isTrashed) {
            sendErrorResponse(output, 404, "Not Found", "Bookmark not found.")
            return
        }

        val col = bookmark.collectionId?.let { collectionRepository.getById(it) }
        val dto = BookmarkDto(
            id = bookmark.id,
            url = bookmark.url,
            title = bookmark.title,
            description = bookmark.description,
            collectionId = bookmark.collectionId,
            collectionName = if (col != null && !col.isPrivate) col.name else null,
            isFavorite = bookmark.isFavorite,
            isPinned = bookmark.isPinned,
            isArchived = bookmark.isArchived,
            visitCount = bookmark.visitCount,
            createdAt = bookmark.createdAt,
            updatedAt = bookmark.updatedAt
        )

        sendJsonResponse(output, 200, "OK", gson.toJson(dto))
    }

    private suspend fun handleGetCollections(output: OutputStream, headers: StringMap) {
        val device = authenticateRequest(headers)
        if (device == null) {
            sendUnauthorizedResponse(output)
            return
        }

        val publicBookmarks = bookmarkRepository.getAllByPrivacy(isPrivate = false)
            .filter { !it.isTrashed }
        val counts = publicBookmarks.groupBy { it.collectionId }

        // Strictly public collections only (isPrivate = false)
        val publicCollections = collectionRepository.getAllByPrivacy(isPrivate = false)
            .map { collection ->
                CollectionDto(
                    id = collection.id,
                    name = collection.name,
                    parentId = collection.parentId,
                    icon = collection.icon,
                    color = collection.color,
                    bookmarkCount = counts[collection.id]?.size ?: 0
                )
            }

        sendJsonResponse(output, 200, "OK", gson.toJson(publicCollections))
    }

    private suspend fun handleRevokeSession(output: OutputStream, headers: StringMap) {
        val device = authenticateRequest(headers)
        if (device == null) {
            sendUnauthorizedResponse(output)
            return
        }

        sessionManager.revokeDevice(device.id)
        val response = mapOf("success" to true, "message" to "Device session revoked successfully.")
        sendJsonResponse(output, 200, "OK", gson.toJson(response))
    }


    private suspend fun authenticateRequest(headers: StringMap): ConnectedDeviceEntity? {
        val authHeader = headers["authorization"] ?: return null
        if (!authHeader.startsWith("Bearer ", ignoreCase = true)) return null
        val token = authHeader.substring(7).trim()
        return sessionManager.authenticateToken(token)
    }

    private fun isAllowedClientIp(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isSiteLocalAddress) return true
        val host = address.hostAddress ?: return false
        return host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")
    }

    private fun isOriginAllowed(origin: String, localIp: String, port: Int): Boolean {
        return origin.startsWith("http://$localIp:$port") ||
                origin.startsWith("http://127.0.0.1:$port") ||
                origin.startsWith("http://localhost:$port")
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, statusText: String, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "X-Frame-Options: DENY\r\n" +
                "Cache-Control: no-store, no-cache, must-revalidate\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Authorization, Content-Type\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendHtmlResponse(output: OutputStream, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "X-Frame-Options: DENY\r\n" +
                "Cache-Control: no-store\r\n" +
                "Content-Security-Policy: default-src 'self' 'unsafe-inline'\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendCorsOptionsResponse(output: OutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Authorization, Content-Type\r\n" +
                "Access-Control-Max-Age: 300\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun sendUnauthorizedResponse(output: OutputStream) {
        val response = ErrorResponse(
            error = "Unauthorized",
            message = "Authentication token missing, expired, or revoked.",
            status = 401
        )
        sendJsonResponse(output, 401, "Unauthorized", gson.toJson(response))
    }

    private fun sendErrorResponse(output: OutputStream, code: Int, status: String, message: String) {
        val response = ErrorResponse(
            error = status,
            message = message,
            status = code
        )
        sendJsonResponse(output, code, status, gson.toJson(response))
    }

    companion object {
        const val DEFAULT_PORT = 8080
        const val MAX_PAYLOAD_BYTES = 65_536 // 64 KB
    }
}

private typealias StringMap = Map<String, String>

// Lightweight built-in HTML client styled in the PixelMarks Warm Editorial Theme
private const val PC_CLIENT_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PixelMarks PC Connect</title>
  <style>
    :root {
      --bg-parchment: #F8F5EE;
      --card-bg: #FFFFFF;
      --charcoal: #2A2624;
      --muted-charcoal: #68615C;
      --light-charcoal: #8E8680;
      --terracotta: #C85A32;
      --terracotta-dark: #A94723;
      --terracotta-soft: #F9EFEB;
      --border: #E5DFD7;
      --border-light: #F0ECE6;
      --accent-bg: #EFE9E0;
      --accent-gold: #C28935;
      --accent-gold-soft: #FCF5EC;
      --success: #2E7D32;
      --success-bg: #E8F5E9;
      --error-bg: #FDECEA;
      --error-text: #C62828;
      --shadow-sm: 0 1px 3px rgba(42, 38, 36, 0.05);
      --shadow-md: 0 4px 14px rgba(42, 38, 36, 0.08);
      --radius: 12px;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: var(--bg-parchment);
      color: var(--charcoal);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      line-height: 1.5;
      padding: 24px 16px;
      min-height: 100vh;
    }
    .container {
      max-width: 1140px;
      margin: 0 auto;
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 20px;
      border-bottom: 2px solid var(--terracotta);
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 16px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .brand-icon {
      width: 40px;
      height: 40px;
      background: var(--terracotta);
      color: white;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: Georgia, serif;
      font-weight: 700;
      font-size: 22px;
      box-shadow: var(--shadow-sm);
    }
    .brand-title {
      font-family: Georgia, Cambria, "Times New Roman", serif;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: -0.5px;
      color: var(--charcoal);
      line-height: 1.1;
    }
    .brand-subtitle {
      color: var(--muted-charcoal);
      font-size: 13px;
      letter-spacing: 0.2px;
      margin-top: 2px;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      background: var(--card-bg);
      border: 1px solid var(--border);
      color: var(--muted-charcoal);
    }
    .status-badge.connected {
      background: var(--success-bg);
      border-color: #C8E6C9;
      color: var(--success);
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #9E9E9E;
    }
    .status-badge.connected .status-dot {
      background: var(--success);
      box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.2);
    }

    /* Pairing Card */
    .pairing-wrapper {
      max-width: 480px;
      margin: 40px auto;
    }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 32px 28px;
      box-shadow: var(--shadow-md);
    }
    .pairing-header {
      text-align: center;
      margin-bottom: 28px;
    }
    .pairing-title {
      font-family: Georgia, serif;
      font-size: 24px;
      font-weight: 700;
      color: var(--charcoal);
      margin-bottom: 8px;
    }
    .pairing-desc {
      color: var(--muted-charcoal);
      font-size: 14px;
      line-height: 1.5;
    }
    .pin-container {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 8px;
      margin: 24px 0 16px;
    }
    .pin-input {
      width: 52px;
      height: 60px;
      text-align: center;
      font-family: Georgia, serif;
      font-size: 28px;
      font-weight: 700;
      color: var(--charcoal);
      border: 2px solid var(--border);
      border-radius: 8px;
      background: #FAFAFA;
      outline: none;
      transition: all 0.2s ease;
    }
    .pin-input:focus {
      border-color: var(--terracotta);
      background: #FFFFFF;
      box-shadow: 0 0 0 3px rgba(200, 90, 50, 0.15);
    }
    .pin-input.has-value {
      border-color: var(--charcoal);
      background: #FFFFFF;
    }
    .pin-divider {
      font-size: 24px;
      font-weight: 700;
      color: var(--light-charcoal);
      user-select: none;
    }
    .pin-hint {
      text-align: center;
      font-size: 12px;
      color: var(--muted-charcoal);
      margin-bottom: 24px;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 10px 18px;
      font-size: 14px;
      font-weight: 600;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
      text-decoration: none;
      border: 1px solid transparent;
      white-space: nowrap;
    }
    .btn-primary {
      background: var(--terracotta);
      color: white;
      border: none;
    }
    .btn-primary:hover { background: var(--terracotta-dark); }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-secondary {
      background: transparent;
      color: var(--charcoal);
      border-color: var(--border);
    }
    .btn-secondary:hover {
      background: var(--accent-bg);
      border-color: #D6CEC4;
    }
    .btn-block { width: 100%; padding: 13px; font-size: 15px; }

    /* Alert / Status Messages */
    .msg-box {
      padding: 14px 16px;
      border-radius: 8px;
      font-size: 14px;
      margin-top: 18px;
      display: none;
      align-items: center;
      gap: 10px;
    }
    .msg-box.visible { display: flex; }
    .msg-info { background: #E8F0FE; color: #174EA6; border: 1px solid #D2E3FC; }
    .msg-error { background: var(--error-bg); color: var(--error-text); border: 1px solid #F5C6CB; }
    .msg-success { background: var(--success-bg); color: var(--success); border: 1px solid #C8E6C9; }

    /* Waiting animation */
    .waiting-box {
      background: var(--terracotta-soft);
      border: 1px solid #F2DDD3;
      border-radius: 8px;
      padding: 18px;
      margin-top: 18px;
      text-align: center;
      display: none;
    }
    .waiting-box.visible { display: block; }
    .spinner {
      width: 28px;
      height: 28px;
      border: 3px solid rgba(200, 90, 50, 0.25);
      border-top-color: var(--terracotta);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 10px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* Dashboard Layout */
    .dashboard-layout {
      display: grid;
      grid-template-columns: 260px 1fr;
      gap: 24px;
      align-items: start;
    }
    @media (max-width: 860px) {
      .dashboard-layout { grid-template-columns: 1fr; }
    }

    /* Sidebar Collections */
    .sidebar {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 18px 14px;
      box-shadow: var(--shadow-sm);
    }
    .sidebar-section-title {
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 1px;
      text-transform: uppercase;
      color: var(--muted-charcoal);
      padding: 6px 12px;
      margin-bottom: 6px;
    }
    .nav-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 14px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 500;
      color: var(--charcoal);
      cursor: pointer;
      transition: all 0.15s ease;
      margin-bottom: 3px;
      user-select: none;
    }
    .nav-item:hover {
      background: var(--accent-bg);
    }
    .nav-item.active {
      background: var(--terracotta-soft);
      color: var(--terracotta);
      font-weight: 600;
    }
    .nav-left {
      display: flex;
      align-items: center;
      gap: 10px;
      overflow: hidden;
    }
    .nav-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .nav-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .nav-count {
      font-size: 12px;
      padding: 2px 7px;
      border-radius: 12px;
      background: var(--accent-bg);
      color: var(--muted-charcoal);
      font-weight: 600;
    }
    .nav-item.active .nav-count {
      background: var(--terracotta);
      color: white;
    }

    /* Main Explorer */
    .explorer-header {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 16px 20px;
      margin-bottom: 16px;
      box-shadow: var(--shadow-sm);
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .search-row {
      display: flex;
      gap: 12px;
      align-items: center;
    }
    .search-wrapper {
      position: relative;
      flex: 1;
    }
    .search-input {
      width: 100%;
      padding: 10px 38px 10px 14px;
      font-size: 14px;
      border: 1px solid var(--border);
      border-radius: 8px;
      outline: none;
      background: #FAFAFA;
      transition: all 0.2s ease;
    }
    .search-input:focus {
      background: #FFFFFF;
      border-color: var(--terracotta);
      box-shadow: 0 0 0 3px rgba(200, 90, 50, 0.12);
    }
    .search-clear {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      font-size: 16px;
      color: var(--light-charcoal);
      cursor: pointer;
      display: none;
    }
    .sort-select {
      padding: 10px 14px;
      font-size: 13px;
      font-weight: 500;
      border: 1px solid var(--border);
      border-radius: 8px;
      background: #FAFAFA;
      color: var(--charcoal);
      outline: none;
      cursor: pointer;
    }
    .filter-summary {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 13px;
      color: var(--muted-charcoal);
      padding-top: 6px;
      border-top: 1px solid var(--border-light);
    }

    /* Bookmark Cards */
    .bookmarks-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .bookmark-card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 20px;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s ease;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
    }
    .bookmark-card:hover {
      border-color: #D6CEC4;
      box-shadow: var(--shadow-md);
      transform: translateY(-1px);
    }
    .bookmark-main {
      flex: 1;
      min-width: 0;
    }
    .bookmark-title-row {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 6px;
      flex-wrap: wrap;
    }
    .bookmark-title {
      font-family: Georgia, serif;
      font-size: 19px;
      font-weight: 700;
      color: var(--charcoal);
      text-decoration: none;
      line-height: 1.3;
      transition: color 0.15s ease;
    }
    .bookmark-title:hover {
      color: var(--terracotta);
    }
    .fav-star {
      color: var(--accent-gold);
      font-size: 15px;
      user-select: none;
    }
    .bookmark-url {
      font-size: 13px;
      color: var(--muted-charcoal);
      word-break: break-all;
      margin-bottom: 8px;
    }
    .bookmark-url a {
      color: inherit;
      text-decoration: none;
    }
    .bookmark-url a:hover {
      text-decoration: underline;
    }
    .bookmark-desc {
      font-size: 14px;
      color: #4A4440;
      line-height: 1.5;
      margin-bottom: 12px;
    }
    .bookmark-meta {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
    .col-tag {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 3px 10px;
      border-radius: 6px;
      background: var(--accent-bg);
      font-size: 12px;
      font-weight: 500;
      color: var(--charcoal);
      cursor: pointer;
      transition: background 0.15s ease;
    }
    .col-tag:hover {
      background: #E5DFD4;
    }
    .col-tag-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
    }
    .date-tag {
      font-size: 12px;
      color: var(--light-charcoal);
    }
    .bookmark-actions {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex-shrink: 0;
      align-items: flex-end;
    }
    .btn-open {
      min-width: 88px;
      white-space: nowrap;
      text-align: center;
    }
    .btn-copy {
      font-size: 12px;
      padding: 6px 12px;
      color: var(--muted-charcoal);
    }

    /* Empty State */
    .empty-state {
      background: var(--card-bg);
      border: 1px dashed var(--border);
      border-radius: var(--radius);
      padding: 48px 24px;
      text-align: center;
      color: var(--muted-charcoal);
    }
    .empty-icon {
      font-size: 40px;
      margin-bottom: 12px;
      opacity: 0.6;
    }
    .empty-title {
      font-family: Georgia, serif;
      font-size: 20px;
      color: var(--charcoal);
      margin-bottom: 6px;
    }
    .hidden { display: none !important; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="brand">
        <div class="brand-icon">P</div>
        <div>
          <div class="brand-title">PixelMarks</div>
          <div class="brand-subtitle">PC Connect &bull; Local Wi-Fi Companion</div>
        </div>
      </div>
      <div class="header-actions">
        <div id="statusBadge" class="status-badge">
          <span class="status-dot"></span>
          <span id="statusText">Disconnected</span>
        </div>
        <button id="refreshBtn" class="btn btn-secondary hidden" title="Sync newest bookmarks from phone">&#8635; Sync</button>
        <button id="disconnectBtn" class="btn btn-secondary hidden">Disconnect</button>
      </div>
    </header>

    <!-- 1. PAIRING SCREEN -->
    <div id="pairingView" class="pairing-wrapper">
      <div class="card">
        <div class="pairing-header">
          <h2 class="pairing-title">Connect to Your Phone</h2>
          <p class="pairing-desc">
            Enter the 6-digit PIN displayed on your PixelMarks Android app to browse and open your bookmarks.
          </p>
        </div>

        <div class="pin-container" id="pinInputs">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="0" autofocus>
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="1">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="2">
          <span class="pin-divider">&ndash;</span>
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="3">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="4">
          <input type="text" class="pin-input" maxlength="1" inputmode="numeric" pattern="[0-9]*" data-idx="5">
        </div>
        <div class="pin-hint">Type or paste the 6 digits &bull; Connection starts automatically</div>

        <button id="connectBtn" class="btn btn-primary btn-block">Connect to Phone</button>

        <div id="waitingBox" class="waiting-box">
          <div class="spinner"></div>
          <strong style="color:var(--terracotta);">Confirm on your phone!</strong>
          <p style="font-size:13px; color:var(--muted-charcoal); margin-top:4px;">
            A dialog appeared on your Android screen. Tap <strong>&ldquo;Allow Connection&rdquo;</strong> to authorize.
          </p>
        </div>

        <div id="errorBox" class="msg-box msg-error"></div>
      </div>
    </div>

    <!-- 2. CONNECTED EXPLORER SCREEN -->
    <div id="dashboardView" class="dashboard-layout hidden">
      <!-- Collections Sidebar -->
      <aside class="sidebar">
        <div class="sidebar-section-title">Navigation</div>
        <div id="navAll" class="nav-item active" onclick="selectCollection(null)">
          <div class="nav-left">
            <span style="font-size:16px;">&#128218;</span>
            <span class="nav-name">All Bookmarks</span>
          </div>
          <span id="allCountBadge" class="nav-count">0</span>
        </div>
        <div id="navFav" class="nav-item" onclick="selectCollection('favorites')">
          <div class="nav-left">
            <span style="color:var(--accent-gold); font-size:16px;">&#9733;</span>
            <span class="nav-name">Favorites</span>
          </div>
          <span id="favCountBadge" class="nav-count">0</span>
        </div>

        <div class="sidebar-section-title" style="margin-top:16px;">Collections</div>
        <div id="collectionsList">
          <!-- Dynamic collections injected here -->
        </div>
      </aside>

      <!-- Main Bookmarks Content -->
      <main>
        <div class="explorer-header">
          <div class="search-row">
            <div class="search-wrapper">
              <input type="text" id="searchInput" class="search-input" placeholder="Search public bookmarks by title, URL, or notes...">
              <button id="searchClearBtn" class="search-clear">&times;</button>
            </div>
            <select id="sortSelect" class="sort-select">
              <option value="newest">Newest First</option>
              <option value="oldest">Oldest First</option>
              <option value="title">Title (A&ndash;Z)</option>
              <option value="visits">Most Visited</option>
            </select>
          </div>
          <div class="filter-summary">
            <span id="currentFilterText">All Public Bookmarks</span>
            <span id="visibleCountText">0 bookmarks</span>
          </div>
        </div>

        <div id="bookmarksList" class="bookmarks-list">
          <!-- Bookmark cards injected here -->
        </div>
      </main>
    </div>
  </div>

  <script>
    // State
    var allBookmarks = [];
    var allCollections = [];
    var activeCollectionId = null; // null = all, 'favorites' = favorites, number = collectionId
    var searchQuery = '';
    var sortOrder = 'newest';

    // DOM Elements
    var pairingView = document.getElementById('pairingView');
    var dashboardView = document.getElementById('dashboardView');
    var pinInputs = Array.from(document.querySelectorAll('.pin-input'));
    var connectBtn = document.getElementById('connectBtn');
    var waitingBox = document.getElementById('waitingBox');
    var errorBox = document.getElementById('errorBox');
    var statusBadge = document.getElementById('statusBadge');
    var statusText = document.getElementById('statusText');
    var refreshBtn = document.getElementById('refreshBtn');
    var disconnectBtn = document.getElementById('disconnectBtn');
    var collectionsList = document.getElementById('collectionsList');
    var bookmarksList = document.getElementById('bookmarksList');
    var searchInput = document.getElementById('searchInput');
    var searchClearBtn = document.getElementById('searchClearBtn');
    var sortSelect = document.getElementById('sortSelect');
    var navAll = document.getElementById('navAll');
    var navFav = document.getElementById('navFav');
    var allCountBadge = document.getElementById('allCountBadge');
    var favCountBadge = document.getElementById('favCountBadge');
    var currentFilterText = document.getElementById('currentFilterText');
    var visibleCountText = document.getElementById('visibleCountText');

    // Utility: HTML Escaper
    function escapeHtml(s) {
      if (!s) return '';
      return String(s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    // Utility: Format Date
    function formatDate(ts) {
      if (!ts) return '';
      try {
        var d = new Date(Number(ts));
        return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
      } catch (_) {
        return '';
      }
    }

    // PIN Input Keyboard & Paste Handling
    pinInputs.forEach(function(input, idx) {
      input.addEventListener('input', function(e) {
        var val = input.value.replace(/[^0-9]/g, '');
        input.value = val ? val.slice(-1) : '';
        if (input.value) {
          input.classList.add('has-value');
          if (idx < pinInputs.length - 1) {
            pinInputs[idx + 1].focus();
          } else {
            // All 6 digits filled -> auto-connect
            var code = getEnteredCode();
            if (code.length === 6) {
              attemptPairing(code);
            }
          }
        } else {
          input.classList.remove('has-value');
        }
      });

      input.addEventListener('keydown', function(e) {
        if (e.key === 'Backspace' && !input.value && idx > 0) {
          pinInputs[idx - 1].focus();
          pinInputs[idx - 1].value = '';
          pinInputs[idx - 1].classList.remove('has-value');
        } else if (e.key === 'ArrowLeft' && idx > 0) {
          pinInputs[idx - 1].focus();
        } else if (e.key === 'ArrowRight' && idx < pinInputs.length - 1) {
          pinInputs[idx + 1].focus();
        } else if (e.key === 'Enter') {
          var code = getEnteredCode();
          if (code.length === 6) attemptPairing(code);
        }
      });

      input.addEventListener('paste', function(e) {
        e.preventDefault();
        var pasteText = (e.clipboardData || window.clipboardData).getData('text') || '';
        var digits = pasteText.replace(/[^0-9]/g, '');
        if (digits.length >= 6) {
          fillCode(digits.slice(0, 6));
          attemptPairing(digits.slice(0, 6));
        } else if (digits.length > 0) {
          fillCode(digits);
        }
      });
    });

    function getEnteredCode() {
      return pinInputs.map(function(i) { return i.value.trim(); }).join('');
    }

    function fillCode(digits) {
      for (var i = 0; i < pinInputs.length; i++) {
        if (i < digits.length) {
          pinInputs[i].value = digits[i];
          pinInputs[i].classList.add('has-value');
        } else {
          pinInputs[i].value = '';
          pinInputs[i].classList.remove('has-value');
        }
      }
      var focusIdx = Math.min(digits.length, pinInputs.length - 1);
      pinInputs[focusIdx].focus();
    }

    function showError(msg) {
      errorBox.textContent = msg;
      errorBox.classList.add('visible');
      waitingBox.classList.remove('visible');
      connectBtn.disabled = false;
    }

    function clearError() {
      errorBox.textContent = '';
      errorBox.classList.remove('visible');
    }

    // Auto-check QR code pairCode in URL
    var urlParams = new URLSearchParams(window.location.search);
    var prefillCode = urlParams.get('pairCode');
    if (prefillCode) {
      var cleanCode = prefillCode.replace(/[^0-9]/g, '');
      if (cleanCode.length === 6) {
        fillCode(cleanCode);
        attemptPairing(cleanCode);
      }
    }

    // Check existing stored token
    var savedToken = sessionStorage.getItem('pixelmarks_token');
    if (savedToken) {
      loadDashboard(savedToken);
    }

    connectBtn.addEventListener('click', function() {
      var code = getEnteredCode();
      if (code.length !== 6) {
        showError('Please enter all 6 digits of the pairing code.');
        var firstEmpty = pinInputs.find(function(i) { return !i.value; });
        if (firstEmpty) firstEmpty.focus();
        return;
      }
      attemptPairing(code);
    });

    async function attemptPairing(code) {
      clearError();
      waitingBox.classList.add('visible');
      connectBtn.disabled = true;

      try {
        var res = await fetch('/api/v1/pair', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            pairingCode: code,
            deviceName: 'PC Browser (' + navigator.platform + ')'
          })
        });
        var data = await res.json();
        if (res.ok && data.token) {
          sessionStorage.setItem('pixelmarks_token', data.token);
          waitingBox.classList.remove('visible');
          loadDashboard(data.token);
        } else {
          showError(data.message || 'Pairing was rejected or timed out. Please try again.');
          pinInputs.forEach(function(i) { i.value = ''; i.classList.remove('has-value'); });
          pinInputs[0].focus();
        }
      } catch (err) {
        showError('Network error connecting to phone: ' + err.message);
      }
    }

    async function loadDashboard(token) {
      statusBadge.className = 'status-badge connected';
      statusText.textContent = 'Connected (Live)';
      refreshBtn.classList.remove('hidden');
      disconnectBtn.classList.remove('hidden');
      pairingView.classList.add('hidden');
      dashboardView.classList.remove('hidden');

      try {
        var [bmRes, colRes] = await Promise.all([
          fetch('/api/v1/bookmarks', { headers: { 'Authorization': 'Bearer ' + token } }),
          fetch('/api/v1/collections', { headers: { 'Authorization': 'Bearer ' + token } })
        ]);

        if (bmRes.status === 401 || colRes.status === 401) {
          sessionStorage.removeItem('pixelmarks_token');
          statusBadge.className = 'status-badge';
          statusText.textContent = 'Session Revoked';
          refreshBtn.classList.add('hidden');
          disconnectBtn.classList.add('hidden');
          dashboardView.classList.add('hidden');
          pairingView.classList.remove('hidden');
          showError('Your PC session was revoked or expired on your phone. Please pair again.');
          return;
        }

        allBookmarks = await bmRes.json();
        allCollections = await colRes.json();

        renderSidebar();
        renderFilteredBookmarks();
      } catch (err) {
        bookmarksList.innerHTML = '<div class="empty-state"><div class="empty-title">Error Loading Bookmarks</div><p>' + escapeHtml(err.message) + '</p></div>';
      }
    }

    function renderSidebar() {
      allCountBadge.textContent = allBookmarks.length;
      var favCount = allBookmarks.filter(function(b) { return b.isFavorite; }).length;
      favCountBadge.textContent = favCount;

      var bmsByCol = {};
      allBookmarks.forEach(function(b) {
        if (b.collectionId) {
          bmsByCol[b.collectionId] = (bmsByCol[b.collectionId] || 0) + 1;
        }
      });

      if (!allCollections || allCollections.length === 0) {
        collectionsList.innerHTML = '<div style="font-size:13px; color:var(--muted-charcoal); padding:8px 12px;">No public collections</div>';
        return;
      }

      collectionsList.innerHTML = allCollections.map(function(c) {
        var count = bmsByCol[c.id] || 0;
        var color = c.color || '#C85A32';
        var isActive = activeCollectionId === c.id;
        return '<div class="nav-item ' + (isActive ? 'active' : '') + '" onclick="selectCollection(' + c.id + ')">' +
          '<div class="nav-left">' +
            '<span class="nav-dot" style="background-color:' + color + ';"></span>' +
            '<span class="nav-name">' + escapeHtml(c.name) + '</span>' +
          '</div>' +
          '<span class="nav-count">' + count + '</span>' +
        '</div>';
      }).join('');
    }

    window.selectCollection = function(colId) {
      activeCollectionId = colId;
      navAll.classList.toggle('active', colId === null);
      navFav.classList.toggle('active', colId === 'favorites');
      renderSidebar();
      renderFilteredBookmarks();
    };

    function renderFilteredBookmarks() {
      var filtered = allBookmarks.slice();

      // Collection filter
      if (activeCollectionId === 'favorites') {
        filtered = filtered.filter(function(b) { return b.isFavorite; });
        currentFilterText.textContent = 'Favorites (' + filtered.length + ')';
      } else if (activeCollectionId !== null) {
        var colObj = allCollections.find(function(c) { return c.id === activeCollectionId; });
        var colName = colObj ? colObj.name : 'Collection';
        filtered = filtered.filter(function(b) { return b.collectionId === activeCollectionId; });
        currentFilterText.textContent = 'Folder: ' + colName + ' (' + filtered.length + ')';
      } else {
        currentFilterText.textContent = 'All Public Bookmarks (' + filtered.length + ')';
      }

      // Search filter
      var q = searchQuery.trim().toLowerCase();
      if (q) {
        filtered = filtered.filter(function(b) {
          return (b.title && b.title.toLowerCase().includes(q)) ||
                 (b.url && b.url.toLowerCase().includes(q)) ||
                 (b.description && b.description.toLowerCase().includes(q)) ||
                 (b.collectionName && b.collectionName.toLowerCase().includes(q));
        });
        currentFilterText.textContent = 'Search: "' + searchQuery + '" (' + filtered.length + ' results)';
      }

      // Sort
      if (sortOrder === 'newest') {
        filtered.sort(function(a, b) { return (b.createdAt || 0) - (a.createdAt || 0); });
      } else if (sortOrder === 'oldest') {
        filtered.sort(function(a, b) { return (a.createdAt || 0) - (b.createdAt || 0); });
      } else if (sortOrder === 'title') {
        filtered.sort(function(a, b) { return (a.title || '').localeCompare(b.title || ''); });
      } else if (sortOrder === 'visits') {
        filtered.sort(function(a, b) { return (b.visitCount || 0) - (a.visitCount || 0); });
      }

      visibleCountText.textContent = filtered.length + ' of ' + allBookmarks.length + ' bookmarks';

      if (filtered.length === 0) {
        bookmarksList.innerHTML = '<div class="empty-state">' +
          '<div class="empty-icon">&#128269;</div>' +
          '<div class="empty-title">No bookmarks found</div>' +
          '<p>No bookmarks match the current filter or search criteria.</p>' +
          (q || activeCollectionId !== null ? '<button class="btn btn-secondary" style="margin-top:14px;" onclick="resetFilters()">View All Bookmarks</button>' : '') +
        '</div>';
        return;
      }

      var colMap = {};
      allCollections.forEach(function(c) { colMap[c.id] = c; });

      bookmarksList.innerHTML = filtered.map(function(b) {
        var col = b.collectionId ? colMap[b.collectionId] : null;
        var colHtml = '';
        if (col) {
          colHtml = '<span class="col-tag" onclick="selectCollection(' + col.id + ')">' +
            '<span class="col-tag-dot" style="background-color:' + (col.color || '#C85A32') + ';"></span>' +
            escapeHtml(col.name) +
          '</span>';
        } else if (b.collectionName) {
          colHtml = '<span class="col-tag">' + escapeHtml(b.collectionName) + '</span>';
        }

        var favHtml = b.isFavorite ? '<span class="fav-star" title="Favorite">&#9733;</span>' : '';
        var descHtml = b.description ? '<div class="bookmark-desc">' + escapeHtml(b.description) + '</div>' : '';
        var dateHtml = b.createdAt ? '<span class="date-tag">Added ' + formatDate(b.createdAt) + '</span>' : '';

        return '<div class="bookmark-card">' +
          '<div class="bookmark-main">' +
            '<div class="bookmark-title-row">' +
              favHtml +
              '<a href="' + escapeHtml(b.url) + '" target="_blank" rel="noopener noreferrer" class="bookmark-title">' +
                escapeHtml(b.title || b.url) +
              '</a>' +
            '</div>' +
            '<div class="bookmark-url">' +
              '<a href="' + escapeHtml(b.url) + '" target="_blank" rel="noopener noreferrer">' + escapeHtml(b.url) + '</a>' +
            '</div>' +
            descHtml +
            '<div class="bookmark-meta">' +
              colHtml +
              dateHtml +
            '</div>' +
          '</div>' +
          '<div class="bookmark-actions">' +
            '<a href="' + escapeHtml(b.url) + '" target="_blank" rel="noopener noreferrer" class="btn btn-primary btn-open">' +
              'Open &rarr;' +
            '</a>' +
            '<button class="btn btn-secondary btn-copy" onclick="copyLink(this, \'' + escapeHtml(b.url) + '\')">' +
              'Copy Link' +
            '</button>' +
          '</div>' +
        '</div>';
      }).join('');
    }

    window.resetFilters = function() {
      activeCollectionId = null;
      searchQuery = '';
      searchInput.value = '';
      searchClearBtn.style.display = 'none';
      navAll.classList.add('active');
      navFav.classList.remove('active');
      renderSidebar();
      renderFilteredBookmarks();
    };

    window.copyLink = function(btn, url) {
      navigator.clipboard.writeText(url).then(function() {
        var prev = btn.textContent;
        btn.textContent = 'Copied!';
        btn.style.color = 'var(--success)';
        setTimeout(function() {
          btn.textContent = prev;
          btn.style.color = '';
        }, 1800);
      });
    };

    searchInput.addEventListener('input', function() {
      searchQuery = searchInput.value;
      searchClearBtn.style.display = searchQuery ? 'block' : 'none';
      renderFilteredBookmarks();
    });

    searchClearBtn.addEventListener('click', function() {
      searchInput.value = '';
      searchQuery = '';
      searchClearBtn.style.display = 'none';
      renderFilteredBookmarks();
      searchInput.focus();
    });

    sortSelect.addEventListener('change', function() {
      sortOrder = sortSelect.value;
      renderFilteredBookmarks();
    });

    refreshBtn.addEventListener('click', function() {
      var t = sessionStorage.getItem('pixelmarks_token');
      if (t) loadDashboard(t);
    });

    disconnectBtn.addEventListener('click', async function() {
      var t = sessionStorage.getItem('pixelmarks_token');
      if (t) {
        try {
          await fetch('/api/v1/revoke', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + t }
          });
        } catch (_) {}
      }
      sessionStorage.removeItem('pixelmarks_token');
      dashboardView.classList.add('hidden');
      refreshBtn.classList.add('hidden');
      disconnectBtn.classList.add('hidden');
      statusBadge.className = 'status-badge';
      statusText.textContent = 'Disconnected';
      pairingView.classList.remove('hidden');
      pinInputs.forEach(function(i) { i.value = ''; i.classList.remove('has-value'); });
      pinInputs[0].focus();
    });
  </script>
</body>
</html>
"""
