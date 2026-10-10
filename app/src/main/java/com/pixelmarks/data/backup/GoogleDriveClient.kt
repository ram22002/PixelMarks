package com.pixelmarks.data.backup

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface GoogleDriveClient {
    suspend fun listBackups(accessToken: String): List<DriveBackupFile>
    suspend fun uploadBackup(accessToken: String, filename: String, inputStream: InputStream, sizeBytes: Long): DriveBackupFile
    suspend fun downloadBackup(accessToken: String, fileId: String, outputStream: OutputStream)
    suspend fun deleteBackup(accessToken: String, fileId: String)
}

class GoogleDriveClientImpl(
    private val httpClient: OkHttpClient = OkHttpClient()
) : GoogleDriveClient {

    private val gson = Gson()
    private val rfc3339Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun parseIsoDate(isoDate: String?): Long {
        if (isoDate.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            rfc3339Format.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            try {
                // Fallback for seconds without millis
                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fallbackFormat.parse(isoDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    override suspend fun listBackups(accessToken: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,size,createdTime,modifiedTime)&orderBy=createdTime%20desc"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.code == 401) {
                throw DriveException.AuthExpired()
            }
            if (!response.isSuccessful) {
                throw DriveException.NetworkError("Failed to list backups: HTTP ${response.code}")
            }

            val bodyString = response.body?.string().orEmpty()
            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
            val filesArray = jsonObject.getAsJsonArray("files") ?: return@withContext emptyList()

            val list = mutableListOf<DriveBackupFile>()
            for (element in filesArray) {
                val fileObj = element.asJsonObject
                val name = fileObj.get("name")?.asString.orEmpty()
                // Only consider .pmb files
                if (name.endsWith(".pmb", ignoreCase = true)) {
                    val id = fileObj.get("id")?.asString.orEmpty()
                    val size = fileObj.get("size")?.asLong ?: 0L
                    val created = parseIsoDate(fileObj.get("createdTime")?.asString)
                    val modified = parseIsoDate(fileObj.get("modifiedTime")?.asString)
                    list.add(
                        DriveBackupFile(
                            id = id,
                            name = name,
                            sizeBytes = size,
                            createdAt = created,
                            modifiedAt = modified
                        )
                    )
                }
            }
            list
        } catch (e: DriveException) {
            throw e
        } catch (e: Exception) {
            throw DriveException.NetworkError("Network error listing Drive backups", e)
        }
    }

    override suspend fun uploadBackup(
        accessToken: String,
        filename: String,
        inputStream: InputStream,
        sizeBytes: Long
    ): DriveBackupFile = withContext(Dispatchers.IO) {
        val metadataJson = """{"name":"$filename","parents":["appDataFolder"]}"""

        val streamBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = sizeBytes
            override fun writeTo(sink: BufferedSink) {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    sink.write(buffer, 0, bytesRead)
                }
            }
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addFormDataPart("file", filename, streamBody)
            .build()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,createdTime,modifiedTime")
            .header("Authorization", "Bearer $accessToken")
            .post(multipartBody)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.code == 401) {
                throw DriveException.AuthExpired()
            }
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string().orEmpty()
                throw DriveException.UploadFailed("Upload failed: HTTP ${response.code} $errorMsg")
            }

            val bodyString = response.body?.string().orEmpty()
            val fileObj = gson.fromJson(bodyString, JsonObject::class.java)
            val id = fileObj.get("id")?.asString.orEmpty()
            val name = fileObj.get("name")?.asString ?: filename
            val size = fileObj.get("size")?.asLong ?: sizeBytes
            val created = parseIsoDate(fileObj.get("createdTime")?.asString)
            val modified = parseIsoDate(fileObj.get("modifiedTime")?.asString)

            DriveBackupFile(
                id = id,
                name = name,
                sizeBytes = size,
                createdAt = created,
                modifiedAt = modified
            )
        } catch (e: DriveException) {
            throw e
        } catch (e: Exception) {
            throw DriveException.UploadFailed("Network error uploading backup to Drive", e)
        }
    }

    override suspend fun downloadBackup(
        accessToken: String,
        fileId: String,
        outputStream: OutputStream
    ): Unit = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.code == 401) {
                throw DriveException.AuthExpired()
            }
            if (response.code == 404) {
                throw DriveException.FileNotFound()
            }
            if (!response.isSuccessful) {
                throw DriveException.DownloadFailed("Download failed: HTTP ${response.code}")
            }

            val inputStream = response.body?.byteStream()
                ?: throw DriveException.DownloadFailed("Empty response body from Drive")

            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
        } catch (e: DriveException) {
            throw e
        } catch (e: Exception) {
            throw DriveException.DownloadFailed("Network error downloading backup from Drive", e)
        }
    }

    override suspend fun deleteBackup(accessToken: String, fileId: String): Unit = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .delete()
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.code == 401) {
                throw DriveException.AuthExpired()
            }
            if (response.code == 404) {
                // Already deleted
                return@withContext
            }
            if (!response.isSuccessful) {
                throw DriveException.NetworkError("Failed to delete cloud backup: HTTP ${response.code}")
            }
        } catch (e: DriveException) {
            throw e
        } catch (e: Exception) {
            throw DriveException.NetworkError("Network error deleting cloud backup", e)
        }
    }
}
