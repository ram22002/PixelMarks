package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Tag
import com.pixelmarks.domain.usecase.ArchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.CreateTagResult
import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.GetBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkByIdUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.PermanentlyDeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.ReplaceBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.RestoreBookmarkUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkFavoriteUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkPinUseCase
import com.pixelmarks.domain.usecase.TrashBookmarkUseCase
import com.pixelmarks.domain.usecase.UnarchiveBookmarkUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookmarkDetailUiState(
    val bookmark: Bookmark? = null,
    val assignedTags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
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
    private val permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BookmarkDetailUiState> = combine(
        observeBookmarkByIdUseCase(bookmarkId),
        observeBookmarkTagsUseCase(bookmarkId),
        observeTagsUseCase(),
        _errorMessage
    ) { bookmark, assignedTags, allTags, err ->
        BookmarkDetailUiState(
            bookmark = bookmark,
            assignedTags = assignedTags,
            allTags = allTags,
            isLoading = bookmark == null,
            errorMessage = err
        )
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
            permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase
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
                    permanentlyDeleteBookmarkUseCase = permanentlyDeleteBookmarkUseCase
                ) as T
            }
        }
    }
}
