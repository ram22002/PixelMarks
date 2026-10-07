package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.Tag
import com.pixelmarks.domain.usecase.ArchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.CancelBookmarkReminderUseCase
import com.pixelmarks.domain.usecase.CreateTagResult
import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.GetBookmarkUseCase
import com.pixelmarks.domain.usecase.MoveBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkByIdUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.PermanentlyDeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.ReplaceBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.RestoreBookmarkUseCase
import com.pixelmarks.domain.usecase.SetBookmarkReminderUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkFavoriteUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkPinUseCase
import com.pixelmarks.domain.usecase.TrashBookmarkUseCase
import com.pixelmarks.domain.usecase.UnarchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.UpdateBookmarkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.pixelmarks.core.model.Reminder
import com.pixelmarks.domain.usecase.ObserveBookmarkReminderUseCase
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.domain.usecase.GetBrowserPreferenceUseCase
import com.pixelmarks.domain.usecase.RecordBookmarkVisitUseCase

data class BookmarkDetailUiState(
    val bookmark: Bookmark? = null,
    val assignedTags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val availableCollections: List<Collection> = emptyList(),
    val assignedCollection: Collection? = null,
    val reminder: Reminder? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BookmarkDetailViewModel(
    private val bookmarkId: Long,
    private val getBookmarkUseCase: GetBookmarkUseCase,
    private val observeBookmarkByIdUseCase: ObserveBookmarkByIdUseCase,
    private val observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase,
    private val toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase,
    private val archiveBookmarkUseCase: ArchiveBookmarkUseCase,
    private val unarchiveBookmarkUseCase: UnarchiveBookmarkUseCase,
    private val trashBookmarkUseCase: TrashBookmarkUseCase,
    private val restoreBookmarkUseCase: RestoreBookmarkUseCase,
    private val permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase,
    private val updateBookmarkUseCase: UpdateBookmarkUseCase? = null,
    private val moveBookmarkUseCase: MoveBookmarkUseCase? = null,
    private val observeCollectionsUseCase: ObserveCollectionsUseCase? = null,
    private val setBookmarkReminderUseCase: SetBookmarkReminderUseCase? = null,
    private val cancelBookmarkReminderUseCase: CancelBookmarkReminderUseCase? = null,
    private val observeBookmarkReminderUseCase: ObserveBookmarkReminderUseCase? = null,
    private val recordBookmarkVisitUseCase: RecordBookmarkVisitUseCase? = null,
    private val getBrowserPreferenceUseCase: GetBrowserPreferenceUseCase? = null
) : ViewModel() {

    fun recordVisit() {
        recordBookmarkVisitUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase(bookmarkId)
            }
        }
    }

    suspend fun getBrowserPreference(): BrowserPreference {
        return getBrowserPreferenceUseCase?.invoke() ?: BrowserPreference.DEFAULT
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _collections = MutableStateFlow<List<Collection>>(emptyList())

    init {
        observeCollectionsUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { cols ->
                    _collections.value = cols
                }
            }
        }
    }

    private val reminderFlow = observeBookmarkReminderUseCase?.invoke(bookmarkId) ?: kotlinx.coroutines.flow.flowOf(null)

    val uiState: StateFlow<BookmarkDetailUiState> = combine(
        observeBookmarkByIdUseCase(bookmarkId),
        observeBookmarkTagsUseCase(bookmarkId),
        observeTagsUseCase(),
        _collections,
        reminderFlow
    ) { bookmark, assignedTags, allTags, collections, reminder ->
        val assignedCol = bookmark?.collectionId?.let { id -> collections.firstOrNull { it.id == id } }
        BookmarkDetailUiState(
            bookmark = bookmark,
            assignedTags = assignedTags,
            allTags = allTags,
            availableCollections = collections,
            assignedCollection = assignedCol,
            reminder = reminder,
            isLoading = bookmark == null,
            errorMessage = null
        )
    }.combine(_errorMessage) { state, err ->
        state.copy(errorMessage = err)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookmarkDetailUiState(isLoading = true)
    )

    fun toggleFavorite() {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            toggleBookmarkFavoriteUseCase(current)
        }
    }

    fun togglePin() {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            toggleBookmarkPinUseCase(current)
        }
    }

    fun toggleArchive() {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            if (current.isArchived) {
                unarchiveBookmarkUseCase(current)
            } else {
                archiveBookmarkUseCase(current)
            }
        }
    }

    fun moveToTrash(onComplete: () -> Unit = {}) {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            trashBookmarkUseCase(current)
            onComplete()
        }
    }

    fun restore(onComplete: () -> Unit = {}) {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            restoreBookmarkUseCase(current)
            onComplete()
        }
    }

    fun deletePermanently(onComplete: () -> Unit = {}) {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            permanentlyDeleteBookmarkUseCase(current)
            onComplete()
        }
    }

    fun updateTags(tagIds: Set<Long>) {
        viewModelScope.launch {
            val success = replaceBookmarkTagsUseCase(bookmarkId, tagIds)
            if (!success) {
                _errorMessage.value = "Failed to update tags"
            }
        }
    }

    fun createTag(name: String, onSuccess: (Tag) -> Unit) {
        viewModelScope.launch {
            when (val result = createTagUseCase(name)) {
                is CreateTagResult.Success -> {
                    onSuccess(result.tag)
                }
                is CreateTagResult.InvalidName -> {
                    _errorMessage.value = result.message
                }
                is CreateTagResult.DuplicateName -> {
                    _errorMessage.value = result.message
                }
                is CreateTagResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun updateNotes(newNotes: String) {
        val current = uiState.value.bookmark ?: return
        viewModelScope.launch {
            val updated = current.copy(
                notes = newNotes.trim().ifEmpty { null },
                updatedAt = System.currentTimeMillis()
            )
            updateBookmarkUseCase?.invoke(updated)
        }
    }

    fun moveToCollection(collectionId: Long?) {
        viewModelScope.launch {
            moveBookmarkUseCase?.invoke(bookmarkId, collectionId)
        }
    }

    fun setReminder(reminderAt: Long?) {
        viewModelScope.launch {
            if (reminderAt != null) {
                setBookmarkReminderUseCase?.invoke(bookmarkId, reminderAt)
            } else {
                cancelBookmarkReminderUseCase?.invoke(bookmarkId)
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(
            bookmarkId: Long,
            getBookmarkUseCase: GetBookmarkUseCase,
            observeBookmarkByIdUseCase: ObserveBookmarkByIdUseCase,
            observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase,
            observeTagsUseCase: ObserveTagsUseCase,
            replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase,
            createTagUseCase: CreateTagUseCase,
            toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase,
            toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase,
            archiveBookmarkUseCase: ArchiveBookmarkUseCase,
            unarchiveBookmarkUseCase: UnarchiveBookmarkUseCase,
            trashBookmarkUseCase: TrashBookmarkUseCase,
            restoreBookmarkUseCase: RestoreBookmarkUseCase,
            permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase,
            updateBookmarkUseCase: UpdateBookmarkUseCase? = null,
            moveBookmarkUseCase: MoveBookmarkUseCase? = null,
            observeCollectionsUseCase: ObserveCollectionsUseCase? = null,
            setBookmarkReminderUseCase: SetBookmarkReminderUseCase? = null,
            cancelBookmarkReminderUseCase: CancelBookmarkReminderUseCase? = null,
            observeBookmarkReminderUseCase: ObserveBookmarkReminderUseCase? = null,
            recordBookmarkVisitUseCase: RecordBookmarkVisitUseCase? = null,
            getBrowserPreferenceUseCase: GetBrowserPreferenceUseCase? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BookmarkDetailViewModel(
                    bookmarkId = bookmarkId,
                    getBookmarkUseCase = getBookmarkUseCase,
                    observeBookmarkByIdUseCase = observeBookmarkByIdUseCase,
                    observeBookmarkTagsUseCase = observeBookmarkTagsUseCase,
                    observeTagsUseCase = observeTagsUseCase,
                    replaceBookmarkTagsUseCase = replaceBookmarkTagsUseCase,
                    createTagUseCase = createTagUseCase,
                    toggleBookmarkFavoriteUseCase = toggleBookmarkFavoriteUseCase,
                    toggleBookmarkPinUseCase = toggleBookmarkPinUseCase,
                    archiveBookmarkUseCase = archiveBookmarkUseCase,
                    unarchiveBookmarkUseCase = unarchiveBookmarkUseCase,
                    trashBookmarkUseCase = trashBookmarkUseCase,
                    restoreBookmarkUseCase = restoreBookmarkUseCase,
                    permanentlyDeleteBookmarkUseCase = permanentlyDeleteBookmarkUseCase,
                    updateBookmarkUseCase = updateBookmarkUseCase,
                    moveBookmarkUseCase = moveBookmarkUseCase,
                    observeCollectionsUseCase = observeCollectionsUseCase,
                    setBookmarkReminderUseCase = setBookmarkReminderUseCase,
                    cancelBookmarkReminderUseCase = cancelBookmarkReminderUseCase,
                    observeBookmarkReminderUseCase = observeBookmarkReminderUseCase,
                    recordBookmarkVisitUseCase = recordBookmarkVisitUseCase,
                    getBrowserPreferenceUseCase = getBrowserPreferenceUseCase
                ) as T
            }
        }
    }
}
