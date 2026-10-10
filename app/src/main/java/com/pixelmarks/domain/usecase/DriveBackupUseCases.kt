package com.pixelmarks.domain.usecase

import android.content.Intent
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.AutoBackupPreferences
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveConnectionState
import com.pixelmarks.core.backup.model.GoogleAccountInfo
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.backup.model.RestoreResult
import com.pixelmarks.data.repository.DriveBackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class ObserveDriveConnectionUseCase(
    private val repository: DriveBackupRepository
) {
    val connectionState: StateFlow<DriveConnectionState> = repository.connectionState
    val connectedAccount: StateFlow<GoogleAccountInfo?> = repository.connectedAccount
}

class ConnectDriveUseCase(
    private val repository: DriveBackupRepository
) {
    fun getSignInIntent(): Intent = repository.getSignInIntent()

    suspend fun handleSignInResult(data: Intent?): Result<GoogleAccountInfo> {
        return repository.handleSignInResult(data)
    }
}

class DisconnectDriveUseCase(
    private val repository: DriveBackupRepository
) {
    suspend operator fun invoke() {
        repository.disconnect()
    }
}

class ListDriveBackupsUseCase(
    private val repository: DriveBackupRepository
) {
    suspend operator fun invoke(): List<DriveBackupFile> {
        return repository.listBackups()
    }
}

class UploadDriveBackupUseCase(
    private val repository: DriveBackupRepository
) {
    suspend operator fun invoke(password: CharArray, customFilename: String? = null): DriveBackupFile {
        return repository.uploadBackup(password, customFilename)
    }
}

class DownloadAndInspectDriveBackupUseCase(
    private val repository: DriveBackupRepository
) {
    suspend operator fun invoke(fileId: String, tempDir: File): Pair<File, BackupHeaderInfo> {
        return repository.downloadAndInspectBackup(fileId, tempDir)
    }
}

class RestoreDriveBackupUseCase(
    private val repository: DriveBackupRepository
) {
    suspend operator fun invoke(tempFile: File, password: CharArray, mode: RestoreMode): RestoreResult {
        return repository.restoreCloudBackup(tempFile, password, mode)
    }
}

class DeleteDriveBackupUseCase(
    private val repository: DriveBackupRepository
) {
    suspend operator fun invoke(fileId: String) {
        repository.deleteBackup(fileId)
    }
}

class ObserveAutoBackupPreferencesUseCase(
    private val repository: DriveBackupRepository
) {
    operator fun invoke(): Flow<AutoBackupPreferences> {
        return repository.autoBackupPreferences
    }
}

class UpdateAutoBackupPreferencesUseCase(
    private val repository: DriveBackupRepository
) {
    suspend fun setEnabled(enabled: Boolean, password: CharArray? = null) {
        repository.setAutoBackupEnabled(enabled, password)
    }

    suspend fun setFrequency(frequency: AutoBackupFrequency) {
        repository.setAutoBackupFrequency(frequency)
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        repository.setWifiOnly(wifiOnly)
    }
}
