package com.pixelmarks.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.domain.usecase.ObserveTrashedBookmarksUseCase
import com.pixelmarks.domain.usecase.PermanentlyDeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.RestoreBookmarkUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface TrashEvent {
    data class ShowMessage(val message: String) : TrashEvent
}

class TrashViewModel(
    observeTrashedBookmarksUseCase: ObserveTrashedBookmarksUseCase,
    private val restoreBookmarkUseCase: RestoreBookmarkUseCase,
    private val permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = observeTrashedBookmarksUseCase()
        .map { bookmarks ->
            TrashUiState(
                bookmarks = bookmarks,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashUiState(isLoading = true)
        )

    private val _events = MutableSharedFlow<TrashEvent>()
    val events: SharedFlow<TrashEvent> = _events.asSharedFlow()

    fun restore(id: Long) {
        viewModelScope.launch {
            restoreBookmarkUseCase(id)
            _events.emit(TrashEvent.ShowMessage("Bookmark restored"))
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch {
            permanentlyDeleteBookmarkUseCase(id)
            _events.emit(TrashEvent.ShowMessage("Bookmark permanently deleted"))
        }
    }

    companion object {
        fun provideFactory(
            observeTrashedBookmarksUseCase: ObserveTrashedBookmarksUseCase,
            restoreBookmarkUseCase: RestoreBookmarkUseCase,
            permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TrashViewModel(
                    observeTrashedBookmarksUseCase,
                    restoreBookmarkUseCase,
                    permanentlyDeleteBookmarkUseCase
                ) as T
            }
        }
    }
}
