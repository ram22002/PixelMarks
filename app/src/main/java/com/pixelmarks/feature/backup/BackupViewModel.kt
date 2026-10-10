package com.pixelmarks.feature.backup

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.backup.crypto.BackupCrypto
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.AutoBackupPreferences
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupExportResult
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.BackupPayload
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveConnectionState
import com.pixelmarks.core.backup.model.GoogleAccountInfo
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.backup.model.RestoreResult
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.domain.usecase.ConnectDriveUseCase
import com.pixelmarks.domain.usecase.CreateBackupUseCase
import com.pixelmarks.domain.usecase.DeleteDriveBackupUseCase
import com.pixelmarks.domain.usecase.DisconnectDriveUseCase
import com.pixelmarks.domain.usecase.DownloadAndInspectDriveBackupUseCase
import com.pixelmarks.domain.usecase.InspectBackupUseCase
import com.pixelmarks.domain.usecase.ListDriveBackupsUseCase
import com.pixelmarks.domain.usecase.ObserveAutoBackupPreferencesUseCase
import com.pixelmarks.domain.usecase.ObserveDriveConnectionUseCase
import com.pixelmarks.domain.usecase.ObserveLatestBackupUseCase
import com.pixelmarks.domain.usecase.RestoreBackupUseCase
import com.pixelmarks.domain.usecase.RestoreDriveBackupUseCase
import com.pixelmarks.domain.usecase.UpdateAutoBackupPreferencesUseCase
import com.pixelmarks.domain.usecase.UploadDriveBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class BackupUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val exportSuccess: BackupExportResult? = null,
    val restoreSuccess: RestoreResult? = null,
    val errorMessage: String? = null,
    val pendingRestorePayload: BackupPayload? = null,
    val pendingFileUri: Uri? = null,
    val pendingHeaderInfo: BackupHeaderInfo? = null,
    val showPasswordPrompt: Boolean = false,
    val showModeSelectionPrompt: Boolean = false,
    val selectedRestoreMode: RestoreMode = RestoreMode.REPLACE,

    // Google Drive state
    val driveConnectionState: DriveConnectionState = DriveConnectionState.DISCONNECTED,
    val driveConnectedAccount: GoogleAccountInfo? = null,
    val driveBackups: List<DriveBackupFile> = emptyList(),
    val isDriveBackupsLoading: Boolean = false,
    val autoBackupPreferences: AutoBackupPreferences = AutoBackupPreferences(),
    val pendingDriveRestoreFile: DriveBackupFile? = null,
    val pendingDriveTempFile: File? = null,
    val showDrivePasswordPrompt: Boolean = false,
    val showDriveModeSelectionPrompt: Boolean = false,
    val showDeleteDriveBackupConfirm: DriveBackupFile? = null,
    val showAutoBackupPasswordDialog: Boolean = false,
    val cloudUploadSuccess: DriveBackupFile? = null
)

class BackupViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val inspectBackupUseCase: InspectBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val observeLatestBackupUseCase: ObserveLatestBackupUseCase,
    private val observeDriveConnectionUseCase: ObserveDriveConnectionUseCase? = null,
    private val connectDriveUseCase: ConnectDriveUseCase? = null,
    private val disconnectDriveUseCase: DisconnectDriveUseCase? = null,
    private val listDriveBackupsUseCase: ListDriveBackupsUseCase? = null,
    private val uploadDriveBackupUseCase: UploadDriveBackupUseCase? = null,
    private val downloadAndInspectDriveBackupUseCase: DownloadAndInspectDriveBackupUseCase? = null,
    private val restoreDriveBackupUseCase: RestoreDriveBackupUseCase? = null,
    private val deleteDriveBackupUseCase: DeleteDriveBackupUseCase? = null,
    private val observeAutoBackupPreferencesUseCase: ObserveAutoBackupPreferencesUseCase? = null,
    private val updateAutoBackupPreferencesUseCase: UpdateAutoBackupPreferencesUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    val latestBackup: StateFlow<BackupRecordEntity?> =
        observeLatestBackupUseCase()
            .map { it.firstOrNull() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    init {
        // Observe Drive connection
        observeDriveConnectionUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase.connectionState.collect { state ->
                    _uiState.value = _uiState.value.copy(driveConnectionState = state)
                    if (state == DriveConnectionState.CONNECTED) {
                        refreshDriveBackups()
                    }
                }
            }
            viewModelScope.launch {
                useCase.connectedAccount.collect { account ->
                    _uiState.value = _uiState.value.copy(driveConnectedAccount = account)
                }
            }
        }

        // Observe Auto Backup preferences
        observeAutoBackupPreferencesUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { prefs ->
                    _uiState.value = _uiState.value.copy(autoBackupPreferences = prefs)
                }
            }
        }
    }

    // --- Local Backup & Restore ---

    fun createBackup(password: CharArray, uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Creating encrypted backup...",
                errorMessage = null
            )
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw BackupException.RestoreFailed("Cannot open output file destination")

                val result = outputStream.use { stream ->
                    createBackupUseCase(password, stream)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    exportSuccess = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to create backup"
                )
            } finally {
                BackupCrypto.clear(password)
            }
        }
    }

    fun onFileSelectedForRestore(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Inspecting backup file...",
                errorMessage = null
            )
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw BackupException.CorruptedFile("Cannot read selected file")

                val header = inputStream.use { stream ->
                    inspectBackupUseCase.readHeader(stream)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    pendingFileUri = uri,
                    pendingHeaderInfo = header,
                    showPasswordPrompt = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Invalid or unreadable backup file"
                )
            }
        }
    }

    fun unlockAndPrepareRestore(password: CharArray, contentResolver: ContentResolver) {
        val uri = _uiState.value.pendingFileUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Verifying password and decrypting...",
                errorMessage = null,
                showPasswordPrompt = false
            )
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw BackupException.CorruptedFile("Cannot read backup file")

                val payload = inputStream.use { stream ->
                    inspectBackupUseCase.decryptAndValidate(password, stream)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    pendingRestorePayload = payload,
                    showModeSelectionPrompt = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to decrypt backup"
                )
            } finally {
                BackupCrypto.clear(password)
            }
        }
    }

    fun selectRestoreMode(mode: RestoreMode) {
        _uiState.value = _uiState.value.copy(selectedRestoreMode = mode)
    }

    fun confirmAndExecuteRestore() {
        val payload = _uiState.value.pendingRestorePayload ?: return
        val mode = _uiState.value.selectedRestoreMode

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Restoring PixelMarks data...",
                showModeSelectionPrompt = false,
                errorMessage = null
            )
            try {
                val result = restoreBackupUseCase(payload, mode)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    restoreSuccess = result,
                    pendingRestorePayload = null,
                    pendingFileUri = null,
                    pendingHeaderInfo = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to restore backup"
                )
            }
        }
    }

    fun dismissPasswordPrompt() {
        _uiState.value = _uiState.value.copy(
            showPasswordPrompt = false,
            pendingFileUri = null,
            pendingHeaderInfo = null
        )
    }

    fun dismissModeSelectionPrompt() {
        _uiState.value = _uiState.value.copy(
            showModeSelectionPrompt = false,
            pendingRestorePayload = null,
            pendingFileUri = null,
            pendingHeaderInfo = null
        )
    }

    // --- Google Drive Integration ---

    fun getGoogleSignInIntent(): Intent? {
        return connectDriveUseCase?.getSignInIntent()
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Connecting Google Drive...",
                errorMessage = null
            )
            try {
                val result = connectDriveUseCase?.handleSignInResult(data)
                if (result != null && result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadingMessage = null
                    )
                    refreshDriveBackups()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadingMessage = null,
                        errorMessage = result?.exceptionOrNull()?.message ?: "Failed to connect Google Drive"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Google Drive connection error"
                )
            }
        }
    }

    fun disconnectGoogleDrive() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Disconnecting Google Drive...",
                errorMessage = null
            )
            try {
                disconnectDriveUseCase?.invoke()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    driveBackups = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Error disconnecting Google Drive"
                )
            }
        }
    }

    fun refreshDriveBackups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDriveBackupsLoading = true)
            try {
                val backups = listDriveBackupsUseCase?.invoke().orEmpty()
                _uiState.value = _uiState.value.copy(
                    isDriveBackupsLoading = false,
                    driveBackups = backups
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDriveBackupsLoading = false,
                    errorMessage = e.message ?: "Failed to fetch cloud backups"
                )
            }
        }
    }

    fun uploadBackupToDrive(password: CharArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Creating encrypted backup & uploading to Google Drive...",
                errorMessage = null
            )
            try {
                val result = uploadDriveBackupUseCase?.invoke(password)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    cloudUploadSuccess = result
                )
                refreshDriveBackups()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to upload cloud backup"
                )
            } finally {
                BackupCrypto.clear(password)
            }
        }
    }

    fun onDriveBackupSelectedForRestore(backupFile: DriveBackupFile, cacheDir: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Downloading and inspecting backup from Google Drive...",
                errorMessage = null
            )
            try {
                val (tempFile, header) = downloadAndInspectDriveBackupUseCase?.invoke(backupFile.id, cacheDir)
                    ?: throw BackupException.CorruptedFile("Cannot inspect cloud backup")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    pendingDriveRestoreFile = backupFile,
                    pendingDriveTempFile = tempFile,
                    pendingHeaderInfo = header,
                    showDrivePasswordPrompt = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to download cloud backup"
                )
            }
        }
    }

    fun unlockAndPrepareDriveRestore(password: CharArray) {
        val tempFile = _uiState.value.pendingDriveTempFile ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Verifying password and decrypting...",
                errorMessage = null,
                showDrivePasswordPrompt = false
            )
            try {
                val payload = tempFile.inputStream().use { stream ->
                    inspectBackupUseCase.decryptAndValidate(password, stream)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    pendingRestorePayload = payload,
                    showDriveModeSelectionPrompt = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to decrypt cloud backup"
                )
            } finally {
                BackupCrypto.clear(password)
            }
        }
    }

    fun confirmAndExecuteDriveRestore() {
        val payload = _uiState.value.pendingRestorePayload ?: return
        val mode = _uiState.value.selectedRestoreMode

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Restoring PixelMarks data from Google Drive...",
                showDriveModeSelectionPrompt = false,
                errorMessage = null
            )
            try {
                val result = restoreBackupUseCase(payload, mode)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    restoreSuccess = result,
                    pendingRestorePayload = null,
                    pendingDriveRestoreFile = null,
                    pendingDriveTempFile = null,
                    pendingHeaderInfo = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to restore cloud backup"
                )
            } finally {
                _uiState.value.pendingDriveTempFile?.delete()
            }
        }
    }

    fun dismissDriveRestore() {
        _uiState.value.pendingDriveTempFile?.delete()
        _uiState.value = _uiState.value.copy(
            showDrivePasswordPrompt = false,
            showDriveModeSelectionPrompt = false,
            pendingDriveRestoreFile = null,
            pendingDriveTempFile = null,
            pendingRestorePayload = null,
            pendingHeaderInfo = null
        )
    }

    fun requestDeleteDriveBackup(backupFile: DriveBackupFile) {
        _uiState.value = _uiState.value.copy(showDeleteDriveBackupConfirm = backupFile)
    }

    fun confirmDeleteDriveBackup() {
        val backupFile = _uiState.value.showDeleteDriveBackupConfirm ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "Deleting cloud backup...",
                showDeleteDriveBackupConfirm = null,
                errorMessage = null
            )
            try {
                deleteDriveBackupUseCase?.invoke(backupFile.id)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null
                )
                refreshDriveBackups()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadingMessage = null,
                    errorMessage = e.message ?: "Failed to delete cloud backup"
                )
            }
        }
    }

    fun dismissDeleteDriveBackup() {
        _uiState.value = _uiState.value.copy(showDeleteDriveBackupConfirm = null)
    }

    // --- Automatic Backup Settings ---

    fun promptAutoBackupPassword() {
        _uiState.value = _uiState.value.copy(showAutoBackupPasswordDialog = true)
    }

    fun dismissAutoBackupPassword() {
        _uiState.value = _uiState.value.copy(showAutoBackupPasswordDialog = false)
    }

    fun setAutoBackupEnabled(enabled: Boolean, password: CharArray? = null) {
        viewModelScope.launch {
            try {
                updateAutoBackupPreferencesUseCase?.setEnabled(enabled, password)
                _uiState.value = _uiState.value.copy(showAutoBackupPasswordDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to update auto backup settings"
                )
            } finally {
                if (password != null) {
                    BackupCrypto.clear(password)
                }
            }
        }
    }

    fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        viewModelScope.launch {
            try {
                updateAutoBackupPreferencesUseCase?.setFrequency(frequency)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to update frequency"
                )
            }
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            try {
                updateAutoBackupPreferencesUseCase?.setWifiOnly(wifiOnly)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to update Wi-Fi preference"
                )
            }
        }
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            exportSuccess = null,
            restoreSuccess = null,
            cloudUploadSuccess = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        fun provideFactory(
            createBackupUseCase: CreateBackupUseCase,
            inspectBackupUseCase: InspectBackupUseCase,
            restoreBackupUseCase: RestoreBackupUseCase,
            observeLatestBackupUseCase: ObserveLatestBackupUseCase,
            observeDriveConnectionUseCase: ObserveDriveConnectionUseCase? = null,
            connectDriveUseCase: ConnectDriveUseCase? = null,
            disconnectDriveUseCase: DisconnectDriveUseCase? = null,
            listDriveBackupsUseCase: ListDriveBackupsUseCase? = null,
            uploadDriveBackupUseCase: UploadDriveBackupUseCase? = null,
            downloadAndInspectDriveBackupUseCase: DownloadAndInspectDriveBackupUseCase? = null,
            restoreDriveBackupUseCase: RestoreDriveBackupUseCase? = null,
            deleteDriveBackupUseCase: DeleteDriveBackupUseCase? = null,
            observeAutoBackupPreferencesUseCase: ObserveAutoBackupPreferencesUseCase? = null,
            updateAutoBackupPreferencesUseCase: UpdateAutoBackupPreferencesUseCase? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BackupViewModel(
                    createBackupUseCase = createBackupUseCase,
                    inspectBackupUseCase = inspectBackupUseCase,
                    restoreBackupUseCase = restoreBackupUseCase,
                    observeLatestBackupUseCase = observeLatestBackupUseCase,
                    observeDriveConnectionUseCase = observeDriveConnectionUseCase,
                    connectDriveUseCase = connectDriveUseCase,
                    disconnectDriveUseCase = disconnectDriveUseCase,
                    listDriveBackupsUseCase = listDriveBackupsUseCase,
                    uploadDriveBackupUseCase = uploadDriveBackupUseCase,
                    downloadAndInspectDriveBackupUseCase = downloadAndInspectDriveBackupUseCase,
                    restoreDriveBackupUseCase = restoreDriveBackupUseCase,
                    deleteDriveBackupUseCase = deleteDriveBackupUseCase,
                    observeAutoBackupPreferencesUseCase = observeAutoBackupPreferencesUseCase,
                    updateAutoBackupPreferencesUseCase = updateAutoBackupPreferencesUseCase
                ) as T
            }
        }
    }
}
