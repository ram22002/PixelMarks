package com.pixelmarks.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Tag
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.DeleteTagUseCase
import com.pixelmarks.domain.usecase.GetTagUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksByTagUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.UpdateTagResult
import com.pixelmarks.domain.usecase.UpdateTagUseCase
import com.pixelmarks.core.security.PrivateModeManager
import com.pixelmarks.core.security.model.PrivacyMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagDetailUiState(
    val tag: Tag? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class TagDetailViewModel(
    private val tagId: Long,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase,
    private val updateTagUseCase: UpdateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val privateModeManager: PrivateModeManager? = null
) : ViewModel() {

    private val _isDeleted = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val isPrivateMode: Flow<Boolean> =
        privateModeManager?.mode?.map { it == PrivacyMode.PRIVATE }
            ?: flowOf(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TagDetailUiState> = isPrivateMode.flatMapLatest { isPrivate ->
        combine(
            observeTagsUseCase(isPrivate = isPrivate),
            observeBookmarksByTagUseCase(tagId, isPrivate = isPrivate),
            _isDeleted,
            _errorMessage
        ) { allTags, bookmarks, isDel, err ->
            val currentTag = allTags.firstOrNull { it.id == tagId }
            TagDetailUiState(
                tag = currentTag,
                bookmarks = bookmarks,
                isLoading = currentTag == null && !isDel,
                isDeleted = isDel,
                errorMessage = err
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TagDetailUiState(isLoading = true)
    )

    fun updateTag(
        newName: String,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = updateTagUseCase(tagId, newName, color)) {
                is UpdateTagResult.Success -> {
                    _errorMessage.value = null
                    onSuccess()
                }
                is UpdateTagResult.InvalidName -> {
                    _errorMessage.value = result.message
                }
                is UpdateTagResult.DuplicateName -> {
                    _errorMessage.value = result.message
                }
                is UpdateTagResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun deleteTag(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = deleteTagUseCase(tagId)
            if (success) {
                _isDeleted.value = true
                onSuccess()
            } else {
                _errorMessage.value = "Failed to delete tag"
            }
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            deleteBookmarkUseCase(bookmarkId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        fun provideFactory(
            tagId: Long,
            observeTagsUseCase: ObserveTagsUseCase,
            observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase,
            updateTagUseCase: UpdateTagUseCase,
            deleteTagUseCase: DeleteTagUseCase,
            deleteBookmarkUseCase: DeleteBookmarkUseCase,
            privateModeManager: PrivateModeManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagDetailViewModel(
                    tagId,
                    observeTagsUseCase,
                    observeBookmarksByTagUseCase,
                    updateTagUseCase,
                    deleteTagUseCase,
                    deleteBookmarkUseCase,
                    privateModeManager
                ) as T
            }
        }
    }
}
