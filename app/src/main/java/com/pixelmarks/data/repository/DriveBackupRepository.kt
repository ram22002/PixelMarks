package com.pixelmarks.data.repository

import android.content.Context
import android.content.Intent
import com.pixelmarks.core.backup.crypto.AutoBackupKeyManager
import com.pixelmarks.core.backup.crypto.BackupCrypto
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.AutoBackupPreferences
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveConnectionState
import com.pixelmarks.core.backup.model.DriveException
import com.pixelmarks.core.backup.model.GoogleAccountInfo
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.backup.model.RestoreResult
import com.pixelmarks.core.backup.worker.GoogleDriveBackupWorker
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType
import com.pixelmarks.data.backup.GoogleAuthManager
import com.pixelmarks.data.backup.GoogleDriveClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

interface DriveBackupRepository {
    val connectionState: StateFlow<DriveConnectionState>
    val connectedAccount: StateFlow<GoogleAccountInfo?>

    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<GoogleAccountInfo>
    suspend fun disconnect()

    suspend fun listBackups(): List<DriveBackupFile>
    suspend fun uploadBackup(password: CharArray, customFilename: String? = null): DriveBackupFile
    suspend fun downloadAndInspectBackup(fileId: String, tempDir: File): Pair<File, BackupHeaderInfo>
    suspend fun restoreCloudBackup(tempFile: File, password: CharArray, mode: RestoreMode): RestoreResult
    suspend fun deleteBackup(fileId: String)

    val autoBackupPreferences: Flow<AutoBackupPreferences>
    suspend fun setAutoBackupEnabled(enabled: Boolean, password: CharArray? = null)
    suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency)
    suspend fun setWifiOnly(wifiOnly: Boolean)
}

class DriveBackupRepositoryImpl(
    private val context: Context,
    private val database: PixelMarksDatabase,
    private val backupRepository: BackupRepository,
    private val googleAuthManager: GoogleAuthManager,
    private val googleDriveClient: GoogleDriveClient,
    private val drivePreferencesRepository: DrivePreferencesRepository,
    private val autoBackupKeyManager: AutoBackupKeyManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DriveBackupRepository {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val operationMutex = Mutex()

    private val _connectionState = MutableStateFlow(DriveConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<DriveConnectionState> = _connectionState.asStateFlow()

    private val _connectedAccount = MutableStateFlow<GoogleAccountInfo?>(null)
    override val connectedAccount: StateFlow<GoogleAccountInfo?> = _connectedAccount.asStateFlow()

    override val autoBackupPreferences: Flow<AutoBackupPreferences> =
        drivePreferencesRepository.autoBackupPreferences

    init {
        // Initialize connection state on startup
        scope.launch {
            val account = googleAuthManager.getConnectedAccount()
            if (account != null) {
                _connectedAccount.value = account
                _connectionState.value = DriveConnectionState.CONNECTED
                drivePreferencesRepository.setConnectedAccountEmail(account.email)
            } else {
                _connectedAccount.value = null
                _connectionState.value = DriveConnectionState.DISCONNECTED
            }
        }
    }

    override fun getSignInIntent(): Intent {
        _connectionState.value = DriveConnectionState.CONNECTING
        return googleAuthManager.getSignInIntent()
    }

    override suspend fun handleSignInResult(data: Intent?): Result<GoogleAccountInfo> = withContext(ioDispatcher) {
        val result = googleAuthManager.handleSignInResult(data)
        if (result.isSuccess) {
            val account = result.getOrNull()
            _connectedAccount.value = account
            _connectionState.value = DriveConnectionState.CONNECTED
            drivePreferencesRepository.setConnectedAccountEmail(account?.email)
        } else {
            _connectedAccount.value = null
            _connectionState.value = DriveConnectionState.DISCONNECTED
        }
        result
    }

    override suspend fun disconnect(): Unit = withContext(ioDispatcher) {
        googleAuthManager.disconnect()
        _connectedAccount.value = null
        _connectionState.value = DriveConnectionState.DISCONNECTED
        drivePreferencesRepository.setConnectedAccountEmail(null)
        drivePreferencesRepository.setAutoBackupEnabled(false)
        autoBackupKeyManager.clearAutoBackupPassword()
        GoogleDriveBackupWorker.cancel(context)
    }

    override suspend fun listBackups(): List<DriveBackupFile> = withContext(ioDispatcher) {
        val account = _connectedAccount.value ?: throw DriveException.NotConnected()
        val token = try {
            googleAuthManager.getAccessToken()
        } catch (e: Exception) {
            _connectionState.value = DriveConnectionState.AUTH_EXPIRED
            throw DriveException.AuthExpired(e.message ?: "Authentication expired")
        }

        try {
            googleDriveClient.listBackups(token)
        } catch (e: DriveException.AuthExpired) {
            _connectionState.value = DriveConnectionState.AUTH_EXPIRED
            throw e
        }
    }

    override suspend fun uploadBackup(
        password: CharArray,
        customFilename: String?
    ): DriveBackupFile = withContext(ioDispatcher) {
        operationMutex.withLock {
            val account = _connectedAccount.value ?: throw DriveException.NotConnected()
            val token = try {
                googleAuthManager.getAccessToken()
            } catch (e: Exception) {
                _connectionState.value = DriveConnectionState.AUTH_EXPIRED
                throw DriveException.AuthExpired(e.message ?: "Authentication expired")
            }

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = customFilename ?: "pixelmarks_backup_$timestampStr.pmb"
            val tempFile = File(context.cacheDir, "upload_${UUID.randomUUID()}.pmb")

            try {
                // 1. Export encrypted local backup to temp file
                val exportResult = tempFile.outputStream().use { stream ->
                    backupRepository.exportBackup(password, stream)
                }

                // 2. Upload file stream to Google Drive
                val uploaded = tempFile.inputStream().use { stream ->
                    googleDriveClient.uploadBackup(
                        accessToken = token,
                        filename = filename,
                        inputStream = stream,
                        sizeBytes = tempFile.length()
                    )
                }

                // 3. Update status & save Google Drive record
                val now = System.currentTimeMillis()
                drivePreferencesRepository.updateLastCloudBackup(now, uploaded.name)

                val record = BackupRecordEntity(
                    type = BackupType.GOOGLE_DRIVE,
                    location = "google_drive/${uploaded.name}",
                    createdAt = now,
                    sizeBytes = uploaded.sizeBytes,
                    checksum = exportResult.checksum,
                    status = BackupStatus.SUCCESS
                )
                database.backupRecordDao().insert(record)

                uploaded
            } catch (e: DriveException.AuthExpired) {
                _connectionState.value = DriveConnectionState.AUTH_EXPIRED
                throw e
            } finally {
                BackupCrypto.clear(password)
                tempFile.delete()
            }
        }
    }

    override suspend fun downloadAndInspectBackup(
        fileId: String,
        tempDir: File
    ): Pair<File, BackupHeaderInfo> = withContext(ioDispatcher) {
        operationMutex.withLock {
            val account = _connectedAccount.value ?: throw DriveException.NotConnected()
            val token = try {
                googleAuthManager.getAccessToken()
            } catch (e: Exception) {
                _connectionState.value = DriveConnectionState.AUTH_EXPIRED
                throw DriveException.AuthExpired(e.message ?: "Authentication expired")
            }

            val tempFile = File(tempDir, "drive_download_${UUID.randomUUID()}.pmb")

            try {
                // 1. Download file stream
                tempFile.outputStream().use { output ->
                    googleDriveClient.downloadBackup(token, fileId, output)
                }

                // 2. Read and validate header
                val header = tempFile.inputStream().use { input ->
                    backupRepository.readBackupHeader(input)
                }

                Pair(tempFile, header)
            } catch (e: DriveException.AuthExpired) {
                _connectionState.value = DriveConnectionState.AUTH_EXPIRED
                tempFile.delete()
                throw e
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }

    override suspend fun restoreCloudBackup(
        tempFile: File,
        password: CharArray,
        mode: RestoreMode
    ): RestoreResult = withContext(ioDispatcher) {
        operationMutex.withLock {
            try {
                if (!tempFile.exists()) {
                    throw BackupException.CorruptedFile("Temporary downloaded backup file not found")
                }

                // 1. Decrypt & validate payload with password
                val payload = tempFile.inputStream().use { stream ->
                    backupRepository.decryptAndValidate(password, stream)
                }

                // 2. Perform transactional database restore
                backupRepository.restoreBackup(payload, mode)
            } finally {
                BackupCrypto.clear(password)
                tempFile.delete()
            }
        }
    }

    override suspend fun deleteBackup(fileId: String): Unit = withContext(ioDispatcher) {
        val account = _connectedAccount.value ?: throw DriveException.NotConnected()
        val token = try {
            googleAuthManager.getAccessToken()
        } catch (e: Exception) {
            _connectionState.value = DriveConnectionState.AUTH_EXPIRED
            throw DriveException.AuthExpired(e.message ?: "Authentication expired")
        }

        try {
            googleDriveClient.deleteBackup(token, fileId)
        } catch (e: DriveException.AuthExpired) {
            _connectionState.value = DriveConnectionState.AUTH_EXPIRED
            throw e
        }
    }

    override suspend fun setAutoBackupEnabled(
        enabled: Boolean,
        password: CharArray?
    ): Unit = withContext(ioDispatcher) {
        if (enabled) {
            if (password != null && password.isNotEmpty()) {
                autoBackupKeyManager.saveAutoBackupPassword(password)
            }
            if (!autoBackupKeyManager.hasAutoBackupPassword()) {
                throw BackupException.ValidationError("An encryption password is required for automatic backups")
            }
            drivePreferencesRepository.setAutoBackupEnabled(true)
            val currentPrefs = drivePreferencesRepository.autoBackupPreferences.first()
            GoogleDriveBackupWorker.schedule(
                context = context,
                frequency = currentPrefs.frequency,
                wifiOnly = currentPrefs.wifiOnly
            )
        } else {
            drivePreferencesRepository.setAutoBackupEnabled(false)
            autoBackupKeyManager.clearAutoBackupPassword()
            GoogleDriveBackupWorker.cancel(context)
        }
    }

    override suspend fun setAutoBackupFrequency(
        frequency: AutoBackupFrequency
    ): Unit = withContext(ioDispatcher) {
        drivePreferencesRepository.setAutoBackupFrequency(frequency)
        val currentPrefs = drivePreferencesRepository.autoBackupPreferences.first()
        if (currentPrefs.enabled) {
            GoogleDriveBackupWorker.schedule(
                context = context,
                frequency = frequency,
                wifiOnly = currentPrefs.wifiOnly
            )
        }
    }

    override suspend fun setWifiOnly(
        wifiOnly: Boolean
    ): Unit = withContext(ioDispatcher) {
        drivePreferencesRepository.setWifiOnly(wifiOnly)
        val currentPrefs = drivePreferencesRepository.autoBackupPreferences.first()
        if (currentPrefs.enabled) {
            GoogleDriveBackupWorker.schedule(
                context = context,
                frequency = currentPrefs.frequency,
                wifiOnly = wifiOnly
            )
        }
    }
}
