package com.pixelmarks.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Default implementation of [MetadataHttpClient] using OkHttp.
 * Configured with strict timeouts, size limits, anonymous headers, and security constraints.
 */
class DefaultMetadataHttpClient(
    private val client: OkHttpClient = createDefaultOkHttpClient()
) : MetadataHttpClient {

    companion object {
        const val MAX_RESPONSE_BYTES = 2L * 1024L * 1024L // 2 MB
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 PixelMarks/1.0"

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    override suspend fun fetchHtml(url: String): HttpResponseResult = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()

        // Protocol security validation
        val lowerUrl = trimmedUrl.lowercase(Locale.ROOT)
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return@withContext HttpResponseResult.Blocked("Unsupported scheme in URL: $trimmedUrl")
        }

        val request = try {
            Request.Builder()
                .url(trimmedUrl)
                .header("User-Agent", DEFAULT_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("DNT", "1")
                .header("Cache-Control", "no-cache")
                .build()
        } catch (e: IllegalArgumentException) {
            return@withContext HttpResponseResult.Failed("Malformed URL: ${e.message}")
        }

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val finalUrl = response.request.url.toString()

                if (!response.isSuccessful) {
                    return@withContext HttpResponseResult.Failed("HTTP $code", statusCode = code)
                }

                val responseBody = response.body
                    ?: return@withContext HttpResponseResult.Success("", finalUrl)

                val mediaType = responseBody.contentType()
                val contentTypeString = mediaType?.toString()?.lowercase(Locale.ROOT) ?: ""

                // Filter out non-HTML content types early
                if (isUnsupportedContentType(contentTypeString)) {
                    return@withContext HttpResponseResult.Blocked("Unsupported content type: $contentTypeString")
                }

                // Check advertised Content-Length header
                val contentLength = responseBody.contentLength()
                if (contentLength > MAX_RESPONSE_BYTES) {
                    return@withContext HttpResponseResult.Blocked("Response exceeded size limit: $contentLength bytes")
                }

                // Read safely with a bounded buffer
                val source = responseBody.source()
                val buffer = Buffer()
                var totalBytesRead = 0L
                val readChunkSize = 8192L

                while (true) {
                    val bytesRead = source.read(buffer, readChunkSize)
                    if (bytesRead == -1L) break
                    totalBytesRead += bytesRead
                    if (totalBytesRead > MAX_RESPONSE_BYTES) {
                        return@withContext HttpResponseResult.Blocked("Response body exceeded 2MB limit")
                    }
                }

                val charset: Charset = mediaType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                val htmlContent = buffer.readString(charset)

                HttpResponseResult.Success(htmlContent, finalUrl)
            }
        } catch (e: SocketTimeoutException) {
            HttpResponseResult.Failed("Connection timed out: ${e.message}")
        } catch (e: UnknownHostException) {
            HttpResponseResult.Failed("DNS resolution failed: ${e.message}")
        } catch (e: SSLException) {
            HttpResponseResult.Failed("TLS/SSL handshake failed: ${e.message}")
        } catch (e: IOException) {
            HttpResponseResult.Failed("I/O error: ${e.message}")
        } catch (e: Exception) {
            HttpResponseResult.Failed("Unexpected error: ${e.message}")
        }
    }

    private fun isUnsupportedContentType(contentType: String): Boolean {
        if (contentType.isEmpty()) return false
        val blockedPrefixes = listOf(
            "image/",
            "video/",
            "audio/",
            "application/octet-stream",
            "application/pdf",
            "application/zip",
            "application/x-rar",
            "application/gzip"
        )
        return blockedPrefixes.any { contentType.startsWith(it) }
    }
}
