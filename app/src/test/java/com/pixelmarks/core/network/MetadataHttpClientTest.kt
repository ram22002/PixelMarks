package com.pixelmarks.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class MetadataHttpClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var httpClient: DefaultMetadataHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .callTimeout(1, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        httpClient = DefaultMetadataHttpClient(okHttpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun fetchHtml_success200() = runBlocking {
        val testHtml = "<html><head><title>Test Page</title></head></html>"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody(testHtml)
        )

        val url = mockWebServer.url("/page").toString()
        val result = httpClient.fetchHtml(url)

        assertTrue(result is HttpResponseResult.Success)
        val success = result as HttpResponseResult.Success
        assertEquals(testHtml, success.html)
        assertEquals(url, success.finalUrl)
    }

    @Test
    fun fetchHtml_http404() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
        )

        val url = mockWebServer.url("/not-found").toString()
        val result = httpClient.fetchHtml(url)

        assertTrue(result is HttpResponseResult.Failed)
        val failed = result as HttpResponseResult.Failed
        assertEquals(404, failed.statusCode)
    }

    @Test
    fun fetchHtml_http403() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
        )

        val url = mockWebServer.url("/forbidden").toString()
        val result = httpClient.fetchHtml(url)

        assertTrue(result is HttpResponseResult.Failed)
        val failed = result as HttpResponseResult.Failed
        assertEquals(403, failed.statusCode)
    }

    @Test
    fun fetchHtml_http500() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        val url = mockWebServer.url("/server-error").toString()
        val result = httpClient.fetchHtml(url)

        assertTrue(result is HttpResponseResult.Failed)
        val failed = result as HttpResponseResult.Failed
        assertEquals(500, failed.statusCode)
    }

    @Test
    fun fetchHtml_redirectFollowed() = runBlocking {
        val targetPath = "/final-destination"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", mockWebServer.url(targetPath).toString())
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html")
                .setBody("<html><head><title>Final</title></head></html>")
        )

        val startUrl = mockWebServer.url("/start").toString()
        val result = httpClient.fetchHtml(startUrl)

        assertTrue(result is HttpResponseResult.Success)
        val success = result as HttpResponseResult.Success
        assertTrue(success.finalUrl.endsWith(targetPath))
    }

    @Test
    fun fetchHtml_unsupportedContentType_isBlocked() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "image/png")
                .setBody("binary-content")
        )

        val url = mockWebServer.url("/image.png").toString()
        val result = httpClient.fetchHtml(url)

        assertTrue(result is HttpResponseResult.Blocked)
    }

    @Test
    fun fetchHtml_oversizedBody_isBlocked() = runBlocking {
        // Exceed 2MB with actual buffered payload
        val buffer = okio.Buffer().apply {
            val oneMb = ByteArray(1024 * 1024)
            write(oneMb)
            write(oneMb)
            write(ByteArray(1024)) // 2 MB + 1 KB
        }
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html")
                .setBody(buffer)
        )

        val url = mockWebServer.url("/large").toString()
        val result = httpClient.fetchHtml(url)

        assertTrue(result is HttpResponseResult.Blocked)
    }

    @Test
    fun fetchHtml_unsupportedScheme_isBlocked() = runBlocking {
        val result = httpClient.fetchHtml("javascript:alert(1)")
        assertTrue(result is HttpResponseResult.Blocked)
    }
}
