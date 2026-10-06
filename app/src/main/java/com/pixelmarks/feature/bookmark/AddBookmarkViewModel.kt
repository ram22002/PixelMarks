package com.pixelmarks.feature.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.domain.usecase.AddBookmarkResult
import com.pixelmarks.domain.usecase.AddBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddBookmarkUiState(
    val url: String = "",
    val title: String = "",
    val selectedCollectionId: Long? = null,
    val availableCollections: List<Collection> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val duplicateBookmark: Bookmark? = null,
    val isSavedSuccessfully: Boolean = false
)

class AddBookmarkViewModel(
    private val addBookmarkUseCase: AddBookmarkUseCase,
    private val observeCollectionsUseCase: ObserveCollectionsUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddBookmarkUiState())
    val uiState: StateFlow<AddBookmarkUiState> = _uiState.asStateFlow()

    init {
        observeCollectionsUseCase?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { collections ->
                    _uiState.update { it.copy(availableCollections = collections) }
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(url = newUrl, errorMessage = null) }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onCollectionSelected(collectionId: Long?) {
        _uiState.update { it.copy(selectedCollectionId = collectionId) }
    }

    fun saveBookmark() {
        val currentUrl = _uiState.value.url.trim()
        if (currentUrl.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "URL cannot be empty") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = addBookmarkUseCase(
                rawUrl = currentUrl,
                customTitle = _uiState.value.title.takeIf { it.isNotBlank() },
                collectionId = _uiState.value.selectedCollectionId,
                forceSave = false
            )) {
                is AddBookmarkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedSuccessfully = true,
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
        _uiState.update { it.copy(isLoading = true, duplicateBookmark = null, errorMessage = null) }
        viewModelScope.launch {
            when (val result = addBookmarkUseCase(
                rawUrl = currentUrl,
                customTitle = _uiState.value.title.takeIf { it.isNotBlank() },
                collectionId = _uiState.value.selectedCollectionId,
                forceSave = true
            )) {
                is AddBookmarkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSavedSuccessfully = true
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
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
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
            observeCollectionsUseCase: ObserveCollectionsUseCase? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AddBookmarkViewModel(addBookmarkUseCase, observeCollectionsUseCase) as T
                }
            }
    }
}
