package com.pixelmarks.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Tag
import com.pixelmarks.domain.usecase.CreateTagResult
import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.DeleteTagUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.UpdateTagResult
import com.pixelmarks.domain.usecase.UpdateTagUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagsUiState(
    val tags: List<Tag> = emptyList(),
    val bookmarkCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TagsViewModel(
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val updateTagUseCase: UpdateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TagsUiState> = combine(
        observeTagsUseCase(),
        observeTagsUseCase.observeTagCounts(),
        _errorState
    ) { tags, counts, error ->
        TagsUiState(
            tags = tags.sortedBy { it.name.lowercase() },
            bookmarkCounts = counts,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TagsUiState(isLoading = true)
    )

    fun createTag(
        name: String,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = createTagUseCase(name, color)) {
                is CreateTagResult.Success -> {
                    _errorState.value = null
                    onSuccess()
                }
                is CreateTagResult.InvalidName -> {
                    _errorState.value = result.message
                }
                is CreateTagResult.DuplicateName -> {
                    _errorState.value = result.message
                }
                is CreateTagResult.Error -> {
                    _errorState.value = result.message
                }
            }
        }
    }

    fun updateTag(
        tagId: Long,
        newName: String,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = updateTagUseCase(tagId, newName, color)) {
                is UpdateTagResult.Success -> {
                    _errorState.value = null
                    onSuccess()
                }
                is UpdateTagResult.InvalidName -> {
                    _errorState.value = result.message
                }
                is UpdateTagResult.DuplicateName -> {
                    _errorState.value = result.message
                }
                is UpdateTagResult.Error -> {
                    _errorState.value = result.message
                }
            }
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            val success = deleteTagUseCase(tagId)
            if (!success) {
                _errorState.value = "Failed to delete tag"
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    companion object {
        fun provideFactory(
            observeTagsUseCase: ObserveTagsUseCase,
            createTagUseCase: CreateTagUseCase,
            updateTagUseCase: UpdateTagUseCase,
            deleteTagUseCase: DeleteTagUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagsViewModel(
                    observeTagsUseCase,
                    createTagUseCase,
                    updateTagUseCase,
                    deleteTagUseCase
                ) as T
            }
        }
    }
}
