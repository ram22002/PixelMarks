package com.pixelmarks.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.domain.usecase.ArchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import com.pixelmarks.domain.usecase.RestoreBookmarkUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkFavoriteUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkPinUseCase
import com.pixelmarks.domain.usecase.TrashBookmarkUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface HomeEvent {
    data class ShowTrashUndo(val bookmarkId: Long, val message: String = "Bookmark moved to Trash") : HomeEvent
}

class HomeViewModel(
    observeBookmarksUseCase: ObserveBookmarksUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase,
    private val toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase,
    private val archiveBookmarkUseCase: ArchiveBookmarkUseCase,
    private val trashBookmarkUseCase: TrashBookmarkUseCase,
    private val restoreBookmarkUseCase: RestoreBookmarkUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeBookmarksUseCase()
        .map { bookmarks ->
            HomeUiState(
                bookmarks = bookmarks,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun toggleFavorite(bookmark: Bookmark) {
        viewModelScope.launch {
            toggleBookmarkFavoriteUseCase(bookmark)
        }
    }

    fun togglePin(bookmark: Bookmark) {
        viewModelScope.launch {
            toggleBookmarkPinUseCase(bookmark)
        }
    }

    fun archiveBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            archiveBookmarkUseCase(bookmark)
        }
    }

    fun moveToTrash(bookmark: Bookmark) {
        viewModelScope.launch {
            trashBookmarkUseCase(bookmark)
            _events.emit(HomeEvent.ShowTrashUndo(bookmark.id))
        }
    }

    fun restoreBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            restoreBookmarkUseCase(bookmarkId)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            deleteBookmarkUseCase(id)
        }
    }

    companion object {
        fun provideFactory(
            observeBookmarksUseCase: ObserveBookmarksUseCase,
            deleteBookmarkUseCase: DeleteBookmarkUseCase,
            toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase,
            toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase,
            archiveBookmarkUseCase: ArchiveBookmarkUseCase,
            trashBookmarkUseCase: TrashBookmarkUseCase,
            restoreBookmarkUseCase: RestoreBookmarkUseCase
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        observeBookmarksUseCase,
                        deleteBookmarkUseCase,
                        toggleBookmarkFavoriteUseCase,
                        toggleBookmarkPinUseCase,
                        archiveBookmarkUseCase,
                        trashBookmarkUseCase,
                        restoreBookmarkUseCase
                    ) as T
                }
            }
    }
}
