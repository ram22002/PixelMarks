package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Tag
import com.pixelmarks.domain.usecase.CreateTagResult
import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.GetBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.ReplaceBookmarkTagsUseCase
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
    private val observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase,
    private val createTagUseCase: CreateTagUseCase
) : ViewModel() {

    private val _bookmark = MutableStateFlow<Bookmark?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        loadBookmark()
    }

    private fun loadBookmark() {
        viewModelScope.launch {
            _bookmark.value = getBookmarkUseCase(bookmarkId)
        }
    }

    val uiState: StateFlow<BookmarkDetailUiState> = combine(
        _bookmark,
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
            observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase,
            observeTagsUseCase: ObserveTagsUseCase,
            replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase,
            createTagUseCase: CreateTagUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BookmarkDetailViewModel(
                    bookmarkId,
                    getBookmarkUseCase,
                    observeBookmarkTagsUseCase,
                    observeTagsUseCase,
                    replaceBookmarkTagsUseCase,
                    createTagUseCase
                ) as T
            }
        }
    }
}
