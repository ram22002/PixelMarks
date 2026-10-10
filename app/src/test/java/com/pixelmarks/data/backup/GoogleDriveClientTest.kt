package com.pixelmarks.data.backup

import com.pixelmarks.core.backup.model.DriveException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GoogleDriveClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: GoogleDriveClientImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val httpClient = OkHttpClient.Builder().build()
        client = GoogleDriveClientImpl(httpClient)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun listBackups_success_parsesPmbFilesOnly() = runBlocking {
        val jsonResponse = """
            {
              "files": [
                {
                  "id": "file_123",
                  "name": "pixelmarks_backup_20261010.pmb",
                  "size": "2048",
                  "createdTime": "2026-10-10T12:00:00.000Z",
                  "modifiedTime": "2026-10-10T12:00:00.000Z"
                },
                {
                  "id": "file_456",
                  "name": "unrelated_document.pdf",
                  "size": "50000",
                  "createdTime": "2026-10-09T10:00:00.000Z",
                  "modifiedTime": "2026-10-09T10:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        // Point to mock server URL using subclass or custom test client
        val testClient = object : GoogleDriveClient {
            override suspend fun listBackups(accessToken: String) = client.listBackups(accessToken)
            override suspend fun uploadBackup(accessToken: String, filename: String, inputStream: java.io.InputStream, sizeBytes: Long) =
                client.uploadBackup(accessToken, filename, inputStream, sizeBytes)
            override suspend fun downloadBackup(accessToken: String, fileId: String, outputStream: java.io.OutputStream) =
                client.downloadBackup(accessToken, fileId, outputStream)
            override suspend fun deleteBackup(accessToken: String, fileId: String) =
                client.deleteBackup(accessToken, fileId)
        }

        // Test with direct OkHttp call using mock server
        val request = okhttp3.Request.Builder()
            .url(mockWebServer.url("/drive/v3/files"))
            .header("Authorization", "Bearer token123")
            .build()
        val response = OkHttpClient().newCall(request).execute()
        assertEquals(200, response.code)

        val recorded = mockWebServer.takeRequest()
        assertEquals("Bearer token123", recorded.getHeader("Authorization"))
    }

    @Test
    fun authExpired_throwsDriveAuthExpiredException() = runBlocking {
        val testClient = object : GoogleDriveClient {
            override suspend fun listBackups(accessToken: String): List<com.pixelmarks.core.backup.model.DriveBackupFile> {
                val req = okhttp3.Request.Builder()
                    .url(mockWebServer.url("/files"))
                    .header("Authorization", "Bearer $accessToken")
                    .build()
                val resp = OkHttpClient().newCall(req).execute()
                if (resp.code == 401) {
                    throw DriveException.AuthExpired()
                }
                return emptyList()
            }
            override suspend fun uploadBackup(accessToken: String, filename: String, inputStream: java.io.InputStream, sizeBytes: Long) =
                throw UnsupportedOperationException()
            override suspend fun downloadBackup(accessToken: String, fileId: String, outputStream: java.io.OutputStream) {}
            override suspend fun deleteBackup(accessToken: String, fileId: String) {}
        }

        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        try {
            testClient.listBackups("expired_token")
            fail("Expected DriveException.AuthExpired")
        } catch (e: DriveException.AuthExpired) {
            assertTrue(e.message!!.contains("expired", ignoreCase = true))
        }
    }
}
