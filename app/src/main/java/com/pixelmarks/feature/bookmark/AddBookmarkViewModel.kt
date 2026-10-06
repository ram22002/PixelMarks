package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.core.model.Tag
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.data.repository.MetadataRepository
import com.pixelmarks.domain.usecase.AddBookmarkResult
import com.pixelmarks.domain.usecase.AddBookmarkUseCase
import com.pixelmarks.domain.usecase.CreateTagResult
import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookmarkUiState(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val domain: String = "",
    val previewThumbnailUrl: String? = null,
    val previewFaviconUrl: String? = null,
    val isLoadingMetadata: Boolean = false,
    val reminderAt: Long? = null,
    val selectedCollectionId: Long? = null,
    val availableCollections: List<Collection> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val availableTags: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val duplicateBookmark: Bookmark? = null,
    val savedBookmarkId: Long? = null,
    val isSavedSuccessfully: Boolean = false
)

class AddBookmarkViewModel(
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val metadataRepository: MetadataRepository? = null,
    private val observeCollectionsUseCase: ObserveCollectionsUseCase? = null,
    private val observeTagsUseCase: ObserveTagsUseCase? = null,
    private val createTagUseCase: CreateTagUseCase? = null,
    initialUrl: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookmarkUiState())
    val uiState: StateFlow<AddBookmarkUiState> = _uiState.asStateFlow()

    private var metadataJob: Job? = null

    init {
        observeCollectionsUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { collections ->
                    _uiState.update { it.copy(availableCollections = collections) }
                }
            }
        }
        observeTagsUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { tags ->
                    _uiState.update { it.copy(availableTags = tags.sortedBy { t -> t.name.lowercase() }) }
                }
            }
        }

        if (!initialUrl.isNullOrBlank()) {
            onUrlChanged(initialUrl)
        }
    }

    fun onUrlChanged(newUrl: String) {
        val trimmed = newUrl.trim()
        val domain = if (UrlUtils.isValidUrl(trimmed)) {
            UrlUtils.extractDomain(UrlUtils.normalizeUrl(trimmed))
        } else ""

        _uiState.update {
            it.copy(
                url = newUrl,
                domain = domain,
                errorMessage = null
            )
        }

        fetchPreviewIfValid(trimmed)
    }

    private fun fetchPreviewIfValid(trimmedUrl: String) {
        if (!UrlUtils.isValidUrl(trimmedUrl)) return
        val normalized = UrlUtils.normalizeUrl(trimmedUrl)

        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMetadata = true) }
            try {
                val result = metadataRepository?.fetchMetadata(normalized)
                if (result is MetadataFetchResult.Success) {
                    val meta = result.metadata
                    _uiState.update { current ->
                        current.copy(
                            isLoadingMetadata = false,
                            title = if (current.title.isBlank() && !meta.title.isNullOrBlank()) meta.title.trim() else current.title,
                            description = if (current.description.isBlank() && !meta.description.isNullOrBlank()) meta.description.trim() else current.description,
                            previewThumbnailUrl = current.previewThumbnailUrl ?: meta.imageUrl,
                            previewFaviconUrl = current.previewFaviconUrl ?: meta.faviconUrl
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingMetadata = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMetadata = false) }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChanged(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun onReminderChanged(timestamp: Long?) {
        _uiState.update { it.copy(reminderAt = timestamp) }
    }

    fun onThumbnailChanged(imageUrl: String?) {
        _uiState.update { it.copy(previewThumbnailUrl = imageUrl) }
    }

    fun onCollectionSelected(collectionId: Long?) {
        _uiState.update { it.copy(selectedCollectionId = collectionId) }
    }

    fun onTagsSelected(tagIds: Set<Long>) {
        _uiState.update { it.copy(selectedTagIds = tagIds) }
    }

    fun onRemoveTag(tagId: Long) {
        _uiState.update { it.copy(selectedTagIds = it.selectedTagIds - tagId) }
    }

    fun createTag(name: String, onCreated: (Tag) -> Unit) {
        if (createTagUseCase == null) return
        viewModelScope.launch {
            when (val result = createTagUseCase(name)) {
                is CreateTagResult.Success -> {
                    onCreated(result.tag)
                }
                is CreateTagResult.InvalidName -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is CreateTagResult.DuplicateName -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is CreateTagResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun saveBookmark() {
        val currentUrl = _uiState.value.url.trim()
        if (currentUrl.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "URL cannot be empty") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = addBookmarkUseCase(
                rawUrl = currentUrl,
                customTitle = _uiState.value.title.takeIf { it.isNotBlank() },
                description = _uiState.value.description.takeIf { it.isNotBlank() },
                notes = _uiState.value.notes.takeIf { it.isNotBlank() },
                thumbnailUrl = _uiState.value.previewThumbnailUrl,
                faviconUrl = _uiState.value.previewFaviconUrl,
                reminderAt = _uiState.value.reminderAt,
                collectionId = _uiState.value.selectedCollectionId,
                tagIds = _uiState.value.selectedTagIds,
                forceSave = false
            )) {
                is AddBookmarkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedSuccessfully = true,
                            savedBookmarkId = result.bookmark.id,
                            duplicateBookmark = null,
                            errorMessage = null
                        )
                    }
                }
                is AddBookmarkResult.Duplicate -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            duplicateBookmark = result.existing
                        )
                    }
                }
                is AddBookmarkResult.InvalidUrl -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is AddBookmarkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun saveAnyway() {
        val currentUrl = _uiState.value.url.trim()
        _uiState.update { it.copy(isLoading = true, duplicateBookmark = null, errorMessage = null) }
        viewModelScope.launch {
            when (val result = addBookmarkUseCase(
                rawUrl = currentUrl,
                customTitle = _uiState.value.title.takeIf { it.isNotBlank() },
                description = _uiState.value.description.takeIf { it.isNotBlank() },
                notes = _uiState.value.notes.takeIf { it.isNotBlank() },
                thumbnailUrl = _uiState.value.previewThumbnailUrl,
                faviconUrl = _uiState.value.previewFaviconUrl,
                reminderAt = _uiState.value.reminderAt,
                collectionId = _uiState.value.selectedCollectionId,
                tagIds = _uiState.value.selectedTagIds,
                forceSave = true
            )) {
                is AddBookmarkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedSuccessfully = true,
                            savedBookmarkId = result.bookmark.id
                        )
                    }
                }
                is AddBookmarkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun dismissDuplicateDialog() {
        _uiState.update { it.copy(duplicateBookmark = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            addBookmarkUseCase: AddBookmarkUseCase,
            metadataRepository: MetadataRepository? = null,
            observeCollectionsUseCase: ObserveCollectionsUseCase? = null,
            observeTagsUseCase: ObserveTagsUseCase? = null,
            createTagUseCase: CreateTagUseCase? = null,
            initialUrl: String? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AddBookmarkViewModel(
                        addBookmarkUseCase = addBookmarkUseCase,
                        metadataRepository = metadataRepository,
                        observeCollectionsUseCase = observeCollectionsUseCase,
                        observeTagsUseCase = observeTagsUseCase,
                        createTagUseCase = createTagUseCase,
                        initialUrl = initialUrl
                    ) as T
                }
            }
    }
}
