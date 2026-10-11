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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.pixelmarks.core.security.PrivateModeManager
import com.pixelmarks.core.security.model.PrivacyMode

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
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val privateModeManager: PrivateModeManager? = null
) : ViewModel() {

    private val _isDeleted = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val isPrivateActiveFlow: Flow<Boolean> =
        privateModeManager?.mode?.map { it == PrivacyMode.PRIVATE } ?: flowOf(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CollectionDetailUiState> = isPrivateActiveFlow.flatMapLatest { isPrivateActive: Boolean ->
        flow {
            val col = getCollectionUseCase(collectionId)
            emit(col)
        }.flatMapLatest { col: Collection? ->
            val colIsPrivate = col?.isPrivate ?: false
            if (col == null || (colIsPrivate && !isPrivateActive)) {
                flowOf(
                    CollectionDetailUiState(
                        collection = null,
                        isLoading = false,
                        errorMessage = "Collection not found"
                    )
                )
            } else {
                combine(
                    observeCollectionsUseCase(includeHidden = true, isPrivate = col.isPrivate),
                    bookmarkRepository.observeAll(isPrivate = col.isPrivate),
                    _isDeleted,
                    _errorMessage
                ) { allCols, allBookmarks, isDel, err ->
                    val current = allCols.firstOrNull { it.id == collectionId }
                    val parent = current?.parentId?.let { pId -> allCols.firstOrNull { it.id == pId } }
                    val children = allCols.filter { it.parentId == collectionId }.sortedBy { it.position }
                    val childIds = children.map { it.id }.toSet()
                    val relevantIds = childIds + collectionId
                    val bookmarks = allBookmarks.filter { it.collectionId in relevantIds }

                    CollectionDetailUiState(
                        collection = current,
                        parentCollection = parent,
                        childCollections = children,
                        bookmarks = bookmarks,
                        allCollections = allCols,
                        isLoading = false,
                        isDeleted = isDel,
                        errorMessage = err
                    )
                }
            }
        }
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
        val isPrivate = uiState.value.collection?.isPrivate ?: false
        viewModelScope.launch {
            when (val result = createCollectionUseCase(name, parentId = collectionId, icon = icon, color = color, isPrivate = isPrivate)) {
                is CreateCollectionResult.Success -> {
                    _errorMessage.value = null
                    onSuccess()
                }
                is CreateCollectionResult.InvalidName -> {
                    _errorMessage.value = result.message
                }
                is CreateCollectionResult.InvalidHierarchy -> {
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
        description: String? = null,
        keywords: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = updateCollectionUseCase(
                collectionId = collectionId,
                newName = newName,
                newParentId = newParentId,
                icon = icon,
                color = color,
                description = description,
                keywords = keywords
            )) {
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
            deleteBookmarkUseCase: DeleteBookmarkUseCase,
            privateModeManager: PrivateModeManager? = null
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
                    deleteBookmarkUseCase,
                    privateModeManager
                ) as T
            }
        }
    }
}
