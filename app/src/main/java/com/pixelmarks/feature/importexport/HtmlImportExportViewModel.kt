package com.pixelmarks.feature.importexport

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.html.HtmlExportResult
import com.pixelmarks.core.model.html.HtmlExportScope
import com.pixelmarks.core.model.html.HtmlImportCollectionTarget
import com.pixelmarks.core.model.html.HtmlImportDuplicatePolicy
import com.pixelmarks.core.model.html.HtmlImportOptions
import com.pixelmarks.core.model.html.HtmlImportPreview
import com.pixelmarks.core.model.html.HtmlImportSummary
import com.pixelmarks.core.model.html.HtmlParseResult
import com.pixelmarks.core.security.PrivateModeManager
import com.pixelmarks.core.security.model.PrivacyMode
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.domain.usecase.ExportBookmarksHtmlUseCase
import com.pixelmarks.domain.usecase.GetHtmlImportPreviewUseCase
import com.pixelmarks.domain.usecase.ImportBookmarksHtmlUseCase
import com.pixelmarks.domain.usecase.ParseBookmarksHtmlUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtmlImportExportUiState(
    val isPrivate: Boolean = false,
    val availableCollections: List<Collection> = emptyList(),
    // Export state
    val exportScope: HtmlExportScope = HtmlExportScope.All,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportErrorMessage: String? = null,
    // Import state
    val isParsing: Boolean = false,
    val parseResult: HtmlParseResult? = null,
    val importPreview: HtmlImportPreview? = null,
    val importOptions: HtmlImportOptions = HtmlImportOptions(),
    val isImporting: Boolean = false,
    val importProgress: Pair<Int, Int>? = null,
    val importSummary: HtmlImportSummary? = null,
    val importErrorMessage: String? = null
)

class HtmlImportExportViewModel(
    private val parseBookmarksHtmlUseCase: ParseBookmarksHtmlUseCase,
    private val getHtmlImportPreviewUseCase: GetHtmlImportPreviewUseCase,
    private val importBookmarksHtmlUseCase: ImportBookmarksHtmlUseCase,
    private val exportBookmarksHtmlUseCase: ExportBookmarksHtmlUseCase,
    private val collectionRepository: CollectionRepository,
    private val privateModeManager: PrivateModeManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtmlImportExportUiState())
    val uiState: StateFlow<HtmlImportExportUiState> = _uiState.asStateFlow()

    private var activeImportJob: Job? = null

    init {
        viewModelScope.launch {
            privateModeManager.mode.collectLatest { privacyMode ->
                val isPrivate = privacyMode == PrivacyMode.PRIVATE
                _uiState.update {
                    it.copy(
                        isPrivate = isPrivate,
                        // Reset transient previews when boundary changes
                        parseResult = null,
                        importPreview = null,
                        importSummary = null,
                        exportSuccessMessage = null,
                        exportErrorMessage = null,
                        importErrorMessage = null
                    )
                }
                loadCollections(isPrivate)
            }
        }
    }

    private fun loadCollections(isPrivate: Boolean) {
        viewModelScope.launch {
            collectionRepository.observeAll(isPrivate).collectLatest { cols ->
                _uiState.update { it.copy(availableCollections = cols) }
            }
        }
    }

    fun onFileSelected(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update {
                it.copy(
                    isParsing = true,
                    parseResult = null,
                    importPreview = null,
                    importErrorMessage = null
                )
            }

            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Unable to open selected file")

                val result = inputStream.use { parseBookmarksHtmlUseCase(it) }
                val isPrivate = _uiState.value.isPrivate
                val preview = getHtmlImportPreviewUseCase(result, isPrivate)

                _uiState.update {
                    it.copy(
                        isParsing = false,
                        parseResult = result,
                        importPreview = preview,
                        importOptions = HtmlImportOptions(
                            duplicatePolicy = HtmlImportDuplicatePolicy.SKIP,
                            collectionTarget = HtmlImportCollectionTarget.PreserveFolders
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isParsing = false,
                        importErrorMessage = e.message ?: "Failed to read bookmark file"
                    )
                }
            }
        }
    }

    fun updateDuplicatePolicy(policy: HtmlImportDuplicatePolicy) {
        _uiState.update {
            it.copy(importOptions = it.importOptions.copy(duplicatePolicy = policy))
        }
    }

    fun updateCollectionTarget(target: HtmlImportCollectionTarget) {
        _uiState.update {
            it.copy(importOptions = it.importOptions.copy(collectionTarget = target))
        }
    }

    fun confirmImport() {
        val result = _uiState.value.parseResult ?: return
        val options = _uiState.value.importOptions
        val isPrivate = _uiState.value.isPrivate

        activeImportJob?.cancel()
        activeImportJob = viewModelScope.launch(ioDispatcher) {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = 0 to result.validBookmarks.size,
                    importErrorMessage = null
                )
            }

            try {
                val summary = importBookmarksHtmlUseCase(
                    parseResult = result,
                    options = options,
                    isPrivate = isPrivate,
                    onProgress = { current, total ->
                        _uiState.update { it.copy(importProgress = current to total) }
                    }
                )

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        parseResult = null,
                        importPreview = null,
                        importSummary = summary
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        importErrorMessage = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelImport() {
        activeImportJob?.cancel()
        activeImportJob = null
        _uiState.update {
            it.copy(
                isImporting = false,
                isParsing = false,
                importProgress = null,
                parseResult = null,
                importPreview = null,
                importErrorMessage = null
            )
        }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(importSummary = null) }
    }

    fun setExportScope(scope: HtmlExportScope) {
        _uiState.update { it.copy(exportScope = scope) }
    }

    fun exportToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportSuccessMessage = null,
                    exportErrorMessage = null
                )
            }

            try {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw IllegalArgumentException("Unable to open output file destination")

                val isPrivate = _uiState.value.isPrivate
                val scope = _uiState.value.exportScope

                val result = outputStream.use { stream ->
                    exportBookmarksHtmlUseCase(scope, isPrivate, stream)
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccessMessage = "Successfully exported ${result.exportedBookmarksCount} bookmarks (${result.exportedCollectionsCount} collections)."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportErrorMessage = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                exportSuccessMessage = null,
                exportErrorMessage = null,
                importErrorMessage = null
            )
        }
    }

    class Factory(
        private val parseBookmarksHtmlUseCase: ParseBookmarksHtmlUseCase,
        private val getHtmlImportPreviewUseCase: GetHtmlImportPreviewUseCase,
        private val importBookmarksHtmlUseCase: ImportBookmarksHtmlUseCase,
        private val exportBookmarksHtmlUseCase: ExportBookmarksHtmlUseCase,
        private val collectionRepository: CollectionRepository,
        private val privateModeManager: PrivateModeManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HtmlImportExportViewModel(
                parseBookmarksHtmlUseCase,
                getHtmlImportPreviewUseCase,
                importBookmarksHtmlUseCase,
                exportBookmarksHtmlUseCase,
                collectionRepository,
                privateModeManager
            ) as T
        }
    }
}
