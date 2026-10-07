package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.core.model.Tag
import com.pixelmarks.core.security.PrivateModeManager
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
    val isPrivate: Boolean = false,
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
    private val privateModeManager: PrivateModeManager? = null,
    initialUrl: String? = null,
    initialCollectionId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookmarkUiState())
    val uiState: StateFlow<AddBookmarkUiState> = _uiState.asStateFlow()

    private var metadataJob: Job? = null
    private var collectionsJob: Job? = null
    private var tagsJob: Job? = null

    init {
        val initialPrivacy = privateModeManager?.isPrivateActive() ?: false
        _uiState.update { it.copy(isPrivate = initialPrivacy) }

        if (initialCollectionId != null && initialCollectionId > 0) {
            _uiState.update { it.copy(selectedCollectionId = initialCollectionId) }
        }

        loadCollectionsAndTags(initialPrivacy)

        if (!initialUrl.isNullOrBlank()) {
            onUrlChanged(initialUrl)
        }
    }

    private fun loadCollectionsAndTags(isPrivate: Boolean) {
        collectionsJob?.cancel()
        tagsJob?.cancel()

        observeCollectionsUseCase?.let { useCase ->
            collectionsJob = viewModelScope.launch {
                useCase(isPrivate = isPrivate).collect { collections ->
                    _uiState.update { it.copy(availableCollections = collections) }
                }
            }
        }
        observeTagsUseCase?.let { useCase ->
            tagsJob = viewModelScope.launch {
                useCase(isPrivate = isPrivate).collect { tags ->
                    _uiState.update { it.copy(availableTags = tags.sortedBy { t -> t.name.lowercase() }) }
                }
            }
        }
    }

    fun onPrivacyChanged(isPrivate: Boolean) {
        if (_uiState.value.isPrivate == isPrivate) return
        _uiState.update {
            it.copy(
                isPrivate = isPrivate,
                selectedCollectionId = null,
                selectedTagIds = emptySet()
            )
        }
        loadCollectionsAndTags(isPrivate)
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

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onDescriptionChanged(newDescription: String) {
        _uiState.update { it.copy(description = newDescription) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun onCollectionSelected(collectionId: Long?) {
        _uiState.update {
            it.copy(selectedCollectionId = if (it.selectedCollectionId == collectionId) null else collectionId)
        }
    }

    fun onTagToggled(tagId: Long) {
        _uiState.update { state ->
            val updated = if (state.selectedTagIds.contains(tagId)) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.copy(selectedTagIds = updated)
        }
    }

    fun onReminderSet(timestamp: Long?) {
        _uiState.update { it.copy(reminderAt = timestamp) }
    }

    fun onReminderChanged(timestamp: Long?) = onReminderSet(timestamp)

    fun onTagsSelected(tagIds: Set<Long>) {
        _uiState.update { it.copy(selectedTagIds = tagIds) }
    }

    fun onRemoveTag(tagId: Long) {
        _uiState.update { it.copy(selectedTagIds = it.selectedTagIds - tagId) }
    }

    fun createTag(name: String, onSuccess: (Tag) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || createTagUseCase == null) return

        viewModelScope.launch {
            when (val result = createTagUseCase(name = trimmed, isPrivate = _uiState.value.isPrivate)) {
                is CreateTagResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            selectedTagIds = state.selectedTagIds + result.tag.id,
                            errorMessage = null
                        )
                    }
                    onSuccess(result.tag)
                }
                is CreateTagResult.DuplicateName -> {
                    val existing = _uiState.value.availableTags.find { it.name.equals(trimmed, ignoreCase = true) }
                    if (existing != null) {
                        _uiState.update { state ->
                            state.copy(selectedTagIds = state.selectedTagIds + existing.id)
                        }
                        onSuccess(existing)
                    }
                }
                is CreateTagResult.InvalidName -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is CreateTagResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun createAndSelectTag(name: String) = createTag(name)

    private fun fetchPreviewIfValid(trimmedUrl: String) {
        if (!UrlUtils.isValidUrl(trimmedUrl) || metadataRepository == null) {
            return
        }

        metadataJob?.cancel()
        metadataJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMetadata = true) }
            val normalized = UrlUtils.normalizeUrl(trimmedUrl)
            when (val result = metadataRepository.fetchMetadata(normalized)) {
                is MetadataFetchResult.Success -> {
                    val meta = result.metadata
                    _uiState.update { state ->
                        val currentTitle = state.title
                        val autoTitle = if (currentTitle.isBlank() && !meta.title.isNullOrBlank()) {
                            meta.title
                        } else currentTitle

                        val currentDesc = state.description
                        val autoDesc = if (currentDesc.isBlank() && !meta.description.isNullOrBlank()) {
                            meta.description
                        } else currentDesc

                        state.copy(
                            title = autoTitle,
                            description = autoDesc,
                            previewThumbnailUrl = meta.imageUrl ?: state.previewThumbnailUrl,
                            previewFaviconUrl = meta.faviconUrl ?: state.previewFaviconUrl,
                            isLoadingMetadata = false
                        )
                    }
                }
                is MetadataFetchResult.Failed -> {
                    _uiState.update { it.copy(isLoadingMetadata = false) }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingMetadata = false) }
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

        val isPrivate = _uiState.value.isPrivate
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
                forceSave = false,
                isPrivate = isPrivate
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
        val isPrivate = _uiState.value.isPrivate
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
                forceSave = true,
                isPrivate = isPrivate
            )) {
                is AddBookmarkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedSuccessfully = true,
                            savedBookmarkId = result.bookmark.id,
                            errorMessage = null
                        )
                    }
                }
                is AddBookmarkResult.Duplicate -> {}
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
            privateModeManager: PrivateModeManager? = null,
            initialUrl: String? = null,
            initialCollectionId: Long? = null
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
                        privateModeManager = privateModeManager,
                        initialUrl = initialUrl,
                        initialCollectionId = initialCollectionId
                    ) as T
                }
            }
    }
}
