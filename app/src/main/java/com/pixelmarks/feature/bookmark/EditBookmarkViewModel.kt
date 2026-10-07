package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.core.model.Tag
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.data.repository.MetadataRepository
import com.pixelmarks.data.repository.ReminderRepository
import com.pixelmarks.data.repository.TagRepository
import com.pixelmarks.domain.usecase.CreateTagResult
import com.pixelmarks.domain.usecase.CreateTagUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.pixelmarks.core.security.PrivateModeManager

data class EditBookmarkUiState(
    val bookmarkId: Long = 0,
    val url: String = "",
    val originalUrl: String = "",
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val domain: String = "",
    val thumbnailUrl: String? = null,
    val faviconUrl: String? = null,
    val isPrivate: Boolean = false,
    val selectedCollectionId: Long? = null,
    val availableCollections: List<Collection> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val availableTags: List<Tag> = emptyList(),
    val reminderAt: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSavedSuccessfully: Boolean = false,
    val originalBookmark: Bookmark? = null
)

class EditBookmarkViewModel(
    private val bookmarkId: Long,
    private val bookmarkRepository: BookmarkRepository,
    private val tagRepository: TagRepository,
    private val collectionRepository: CollectionRepository,
    private val reminderRepository: ReminderRepository? = null,
    private val metadataRepository: MetadataRepository? = null,
    private val createTagUseCase: CreateTagUseCase? = null,
    private val privateModeManager: PrivateModeManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditBookmarkUiState(bookmarkId = bookmarkId))
    val uiState: StateFlow<EditBookmarkUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val bookmark = bookmarkRepository.getById(bookmarkId)
            val isPrivateActive = privateModeManager?.isPrivateActive() ?: false
            if (bookmark == null || (bookmark.isPrivate && !isPrivateActive)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Bookmark not found") }
                return@launch
            }

            val isPrivate = bookmark.isPrivate
            val collections = collectionRepository.observeAll(isPrivate = isPrivate).firstOrNull() ?: emptyList()
            val allTags = tagRepository.observeAll(isPrivate = isPrivate).firstOrNull() ?: emptyList()
            val assignedTags = tagRepository.observeTagsForBookmark(bookmarkId).firstOrNull() ?: emptyList()
            val reminder = reminderRepository?.getByBookmarkId(bookmarkId)

            _uiState.update {
                it.copy(
                    bookmarkId = bookmark.id,
                    url = bookmark.url,
                    originalUrl = bookmark.url,
                    title = bookmark.title,
                    description = bookmark.description ?: "",
                    notes = bookmark.notes ?: "",
                    domain = bookmark.domain,
                    thumbnailUrl = bookmark.thumbnailUrl,
                    faviconUrl = bookmark.faviconUrl,
                    isPrivate = isPrivate,
                    selectedCollectionId = bookmark.collectionId,
                    availableCollections = collections,
                    selectedTagIds = assignedTags.map { t -> t.id }.toSet(),
                    availableTags = allTags.sortedBy { t -> t.name.lowercase() },
                    reminderAt = reminder?.reminderAt ?: bookmark.reminderAt,
                    originalBookmark = bookmark,
                    isLoading = false
                )
            }
        }
    }

    fun requestTogglePrivacy(newIsPrivate: Boolean) {
        if (_uiState.value.isPrivate == newIsPrivate) return
        viewModelScope.launch {
            val collections = collectionRepository.observeAll(isPrivate = newIsPrivate).firstOrNull() ?: emptyList()
            val allTags = tagRepository.observeAll(isPrivate = newIsPrivate).firstOrNull() ?: emptyList()
            _uiState.update {
                it.copy(
                    isPrivate = newIsPrivate,
                    selectedCollectionId = null,
                    selectedTagIds = emptySet(),
                    availableCollections = collections,
                    availableTags = allTags.sortedBy { t -> t.name.lowercase() }
                )
            }
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

    fun onImageReplaced(newImageUrl: String) {
        _uiState.update { it.copy(thumbnailUrl = newImageUrl) }
    }

    fun onImageRemoved() {
        _uiState.update { it.copy(thumbnailUrl = null) }
    }

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

    fun saveBookmark() {
        val current = _uiState.value.originalBookmark ?: return
        val currentUrl = _uiState.value.url.trim()

        if (currentUrl.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "URL cannot be empty") }
            return
        }

        if (!UrlUtils.isValidUrl(currentUrl)) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid web URL (e.g. example.com)") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val normalized = UrlUtils.normalizeUrl(currentUrl)
                val urlChanged = normalized != current.normalizedUrl

                // Duplicate collision check if URL changed
                if (urlChanged) {
                    val duplicates = bookmarkRepository.findByNormalizedUrl(normalized, isPrivate = _uiState.value.isPrivate)
                    val duplicateOfAnother = duplicates.firstOrNull { it.id != bookmarkId }
                    if (duplicateOfAnother != null) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "Another bookmark already exists for this URL: ${duplicateOfAnother.title}"
                            )
                        }
                        return@launch
                    }
                }

                val now = System.currentTimeMillis()
                val updatedDomain = UrlUtils.extractDomain(normalized)
                val finalTitle = _uiState.value.title.trim().ifEmpty { updatedDomain.ifEmpty { normalized } }

                val updatedBookmark = current.copy(
                    url = normalized,
                    normalizedUrl = normalized,
                    title = finalTitle,
                    description = _uiState.value.description.trim().ifEmpty { null },
                    domain = updatedDomain,
                    thumbnailUrl = _uiState.value.thumbnailUrl?.trim()?.ifEmpty { null },
                    collectionId = _uiState.value.selectedCollectionId,
                    isPrivate = _uiState.value.isPrivate,
                    notes = _uiState.value.notes.trim().ifEmpty { null },
                    reminderAt = _uiState.value.reminderAt,
                    updatedAt = now
                )

                bookmarkRepository.update(updatedBookmark)
                tagRepository.setTagsForBookmark(bookmarkId, _uiState.value.selectedTagIds)

                // Manage reminder synchronization
                if (_uiState.value.reminderAt != null) {
                    reminderRepository?.setReminder(bookmarkId, _uiState.value.reminderAt!!)
                } else {
                    reminderRepository?.cancelReminder(bookmarkId)
                }

                // If URL changed, fetch metadata in background to update metadata if needed
                if (urlChanged && metadataRepository != null) {
                    launch {
                        try {
                            val result = metadataRepository.fetchMetadata(normalized)
                            if (result is MetadataFetchResult.Success) {
                                val meta = result.metadata
                                val latest = bookmarkRepository.getById(bookmarkId)
                                if (latest != null) {
                                    val enriched = latest.copy(
                                        faviconUrl = latest.faviconUrl ?: meta.faviconUrl,
                                        thumbnailUrl = latest.thumbnailUrl ?: meta.imageUrl,
                                        metadataUpdatedAt = System.currentTimeMillis()
                                    )
                                    bookmarkRepository.update(enriched)
                                }
                            }
                        } catch (_: Exception) {
                            // Enrichment failure must not break save
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSavedSuccessfully = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to update bookmark"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            bookmarkId: Long,
            bookmarkRepository: BookmarkRepository,
            tagRepository: TagRepository,
            collectionRepository: CollectionRepository,
            reminderRepository: ReminderRepository? = null,
            metadataRepository: MetadataRepository? = null,
            createTagUseCase: CreateTagUseCase? = null,
            privateModeManager: PrivateModeManager? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return EditBookmarkViewModel(
                        bookmarkId = bookmarkId,
                        bookmarkRepository = bookmarkRepository,
                        tagRepository = tagRepository,
                        collectionRepository = collectionRepository,
                        reminderRepository = reminderRepository,
                        metadataRepository = metadataRepository,
                        createTagUseCase = createTagUseCase,
                        privateModeManager = privateModeManager
                    ) as T
                }
            }
    }
}
