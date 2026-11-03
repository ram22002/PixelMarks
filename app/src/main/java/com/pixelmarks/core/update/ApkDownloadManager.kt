package com.pixelmarks.core.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.pixelmarks.core.update.model.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Download progress state for an in-progress APK download. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progressPercent: Int, val downloadId: Long) : DownloadState
    data class Completed(val localUri: Uri, val downloadId: Long) : DownloadState
    data class Failed(val reason: String) : DownloadState
    data object Cancelled : DownloadState
}

/**
 * Downloads APK assets using the system [DownloadManager].
 *
 * Uses the system download infrastructure for:
 * - Background downloading with progress
 * - Notification-based progress (system-provided)
 * - Download resumption
 * - Proper scoped storage handling
 *
 * Downloads to the app's external files Downloads directory so the
 * PackageInstaller can access the file without a FileProvider.
 */
class ApkDownloadManager(private val context: Context) {

    private val downloadManager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /**
     * Enqueue a download for [asset] and emit [DownloadState] updates as a Flow.
     * The flow completes when the download finishes, fails, or is cancelled.
     */
    fun downloadApk(asset: ReleaseAsset, versionName: String): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        val fileName = "PixelMarks-$versionName.apk"

        val request = DownloadManager.Request(Uri.parse(asset.downloadUrl))
            .setTitle("PixelMarks $versionName")
            .setDescription("Downloading update…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downloadId = downloadManager.enqueue(request)
        emit(DownloadState.Downloading(progressPercent = 0, downloadId = downloadId))

        // Poll the DownloadManager for status
        val query = DownloadManager.Query().setFilterById(downloadId)
        while (true) {
            delay(500)
            val cursor = downloadManager.query(query)
            if (cursor == null || !cursor.moveToFirst()) {
                emit(DownloadState.Failed("Download record not found"))
                return@flow
            }

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            cursor.close()

            when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> {
                    val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    emit(DownloadState.Downloading(progressPercent = progress, downloadId = downloadId))
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val localUri = downloadManager.getUriForDownloadedFile(downloadId)
                    if (localUri != null) {
                        emit(DownloadState.Completed(localUri = localUri, downloadId = downloadId))
                    } else {
                        emit(DownloadState.Failed("Downloaded file URI not found"))
                    }
                    return@flow
                }
                DownloadManager.STATUS_FAILED -> {
                    val errorQuery = DownloadManager.Query().setFilterById(downloadId)
                    val errCursor = downloadManager.query(errorQuery)
                    val reason = if (errCursor != null && errCursor.moveToFirst()) {
                        val r = errCursor.getInt(errCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        errCursor.close()
                        "DownloadManager error code: $r"
                    } else "Unknown download error"
                    emit(DownloadState.Failed(reason))
                    return@flow
                }
                else -> {
                    emit(DownloadState.Failed("Unexpected download status: $status"))
                    return@flow
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /** Cancel an in-progress download. */
    fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }
}

