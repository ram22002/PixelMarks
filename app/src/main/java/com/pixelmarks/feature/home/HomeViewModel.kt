package com.pixelmarks.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    observeBookmarksUseCase: ObserveBookmarksUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase
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

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            deleteBookmarkUseCase(id)
        }
    }

    companion object {
        fun provideFactory(
            observeBookmarksUseCase: ObserveBookmarksUseCase,
            deleteBookmarkUseCase: DeleteBookmarkUseCase
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(observeBookmarksUseCase, deleteBookmarkUseCase) as T
                }
            }
    }
}
