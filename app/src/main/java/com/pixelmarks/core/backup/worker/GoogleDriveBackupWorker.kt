package com.pixelmarks.core.backup.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pixelmarks.app.PixelMarksApplication
import com.pixelmarks.core.backup.crypto.BackupCrypto
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

class GoogleDriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val UNIQUE_WORK_NAME = "pixelmarks_google_drive_autobackup"

        fun schedule(context: Context, frequency: AutoBackupFrequency, wifiOnly: Boolean) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val intervalDays = frequency.intervalDays
                val request = PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(
                    intervalDays,
                    TimeUnit.DAYS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (_: Exception) {}
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            } catch (_: Exception) {}
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? PixelMarksApplication ?: return Result.failure()
        val container = app.container

        // 1. Check if auto backup is enabled
        val drivePrefs = container.drivePreferencesRepository.autoBackupPreferences.first()
        if (!drivePrefs.enabled) {
            return Result.success()
        }

        // 2. Check connected Google account
        val account = container.googleAuthManager.getConnectedAccount() ?: return Result.failure()

        // 3. Retrieve auto backup password securely from KeyStore
        val password = container.autoBackupKeyManager.getAutoBackupPassword() ?: return Result.failure()

        var tempFile: File? = null
        return try {
            // 4. Get Drive access token
            val token = container.googleAuthManager.getAccessToken()

            // 5. Create local encrypted .pmb backup into temp file
            val timeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "pixelmarks_autobackup_$timeFormat.pmb"
            tempFile = File(applicationContext.cacheDir, "autobackup_${UUID.randomUUID()}.pmb")

            val exportResult = tempFile.outputStream().use { stream ->
                container.backupRepository.exportBackup(password, stream)
            }

            // 6. Upload encrypted file to Drive
            val uploadedFile = tempFile.inputStream().use { stream ->
                container.googleDriveClient.uploadBackup(
                    accessToken = token,
                    filename = filename,
                    inputStream = stream,
                    sizeBytes = tempFile.length()
                )
            }

            // 7. Update preferences & record in database
            val now = System.currentTimeMillis()
            container.drivePreferencesRepository.updateLastCloudBackup(now, uploadedFile.name)

            val record = BackupRecordEntity(
                type = BackupType.GOOGLE_DRIVE,
                location = "google_drive/${uploadedFile.name}",
                createdAt = now,
                sizeBytes = uploadedFile.sizeBytes,
                checksum = exportResult.checksum,
                status = BackupStatus.SUCCESS
            )
            container.database.backupRecordDao().insert(record)

            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        } finally {
            BackupCrypto.clear(password)
            tempFile?.delete()
        }
    }
}
