package com.pixelmarks.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.domain.usecase.CreateCollectionResult
import com.pixelmarks.domain.usecase.CreateCollectionUseCase
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.DeleteCollectionUseCase
import com.pixelmarks.domain.usecase.GetCollectionUseCase
import com.pixelmarks.domain.usecase.MoveBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.UpdateCollectionResult
import com.pixelmarks.domain.usecase.UpdateCollectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionDetailUiState(
    val collection: Collection? = null,
    val parentCollection: Collection? = null,
    val childCollections: List<Collection> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val allCollections: List<Collection> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class CollectionDetailViewModel(
    private val collectionId: Long,
    private val getCollectionUseCase: GetCollectionUseCase,
    private val observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val bookmarkRepository: BookmarkRepository,
    private val createCollectionUseCase: CreateCollectionUseCase,
    private val updateCollectionUseCase: UpdateCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val moveBookmarkUseCase: MoveBookmarkUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase
) : ViewModel() {

    private val _isDeleted = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        observeCollectionsUseCase(),
        bookmarkRepository.observeByCollection(collectionId),
        _isDeleted,
        _errorMessage
    ) { allCols, bookmarks, isDel, err ->
        val current = allCols.firstOrNull { it.id == collectionId }
        val parent = current?.parentId?.let { pId -> allCols.firstOrNull { it.id == pId } }
        val children = allCols.filter { it.parentId == collectionId }.sortedBy { it.position }

        CollectionDetailUiState(
            collection = current,
            parentCollection = parent,
            childCollections = children,
            bookmarks = bookmarks,
            allCollections = allCols,
            isLoading = current == null && !isDel,
            isDeleted = isDel,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionDetailUiState(isLoading = true)
    )

    fun createChildCollection(
        name: String,
        icon: String? = null,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = createCollectionUseCase(name, parentId = collectionId, icon, color)) {
                is CreateCollectionResult.Success -> {
                    _errorMessage.value = null
                    onSuccess()
                }
                is CreateCollectionResult.InvalidName -> {
                    _errorMessage.value = result.message
                }
                is CreateCollectionResult.DuplicateName -> {
                    _errorMessage.value = result.message
                }
                is CreateCollectionResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun updateCollection(
        newName: String,
        newParentId: Long? = null,
        icon: String? = null,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = updateCollectionUseCase(collectionId, newName, newParentId, icon, color)) {
                is UpdateCollectionResult.Success -> {
                    _errorMessage.value = null
                    onSuccess()
                }
                is UpdateCollectionResult.InvalidName -> {
                    _errorMessage.value = result.message
                }
                is UpdateCollectionResult.InvalidHierarchy -> {
                    _errorMessage.value = result.message
                }
                is UpdateCollectionResult.DuplicateName -> {
                    _errorMessage.value = result.message
                }
                is UpdateCollectionResult.Error -> {
                    _errorMessage.value = result.message
                }
            }
        }
    }

    fun deleteCollection(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val success = deleteCollectionUseCase(collectionId)
            if (success) {
                _isDeleted.value = true
                onSuccess()
            } else {
                _errorMessage.value = "Failed to delete collection"
            }
        }
    }

    fun moveBookmark(bookmarkId: Long, targetCollectionId: Long?) {
        viewModelScope.launch {
            val success = moveBookmarkUseCase(bookmarkId, targetCollectionId)
            if (!success) {
                _errorMessage.value = "Failed to move bookmark"
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
            collectionId: Long,
            getCollectionUseCase: GetCollectionUseCase,
            observeCollectionsUseCase: ObserveCollectionsUseCase,
            bookmarkRepository: BookmarkRepository,
            createCollectionUseCase: CreateCollectionUseCase,
            updateCollectionUseCase: UpdateCollectionUseCase,
            deleteCollectionUseCase: DeleteCollectionUseCase,
            moveBookmarkUseCase: MoveBookmarkUseCase,
            deleteBookmarkUseCase: DeleteBookmarkUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CollectionDetailViewModel(
                    collectionId,
                    getCollectionUseCase,
                    observeCollectionsUseCase,
                    bookmarkRepository,
                    createCollectionUseCase,
                    updateCollectionUseCase,
                    deleteCollectionUseCase,
                    moveBookmarkUseCase,
                    deleteBookmarkUseCase
                ) as T
            }
        }
    }
}
