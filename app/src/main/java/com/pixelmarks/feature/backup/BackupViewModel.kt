package com.pixelmarks.feature.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.backup.crypto.BackupCrypto
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupExportResult
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.BackupPayload
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.backup.model.RestoreResult
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.domain.usecase.CreateBackupUseCase
import com.pixelmarks.domain.usecase.InspectBackupUseCase
import com.pixelmarks.domain.usecase.ObserveLatestBackupUseCase
import com.pixelmarks.domain.usecase.RestoreBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val selectedRestoreMode: RestoreMode = RestoreMode.REPLACE
)

class BackupViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val inspectBackupUseCase: InspectBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val observeLatestBackupUseCase: ObserveLatestBackupUseCase
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

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            exportSuccess = null,
            restoreSuccess = null
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
            observeLatestBackupUseCase: ObserveLatestBackupUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BackupViewModel(
                    createBackupUseCase = createBackupUseCase,
                    inspectBackupUseCase = inspectBackupUseCase,
                    restoreBackupUseCase = restoreBackupUseCase,
                    observeLatestBackupUseCase = observeLatestBackupUseCase
                ) as T
            }
        }
    }
}
