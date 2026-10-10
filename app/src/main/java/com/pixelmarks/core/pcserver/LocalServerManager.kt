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
                "Content-Security-Policy: default-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none';\r\n\r\n"
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
