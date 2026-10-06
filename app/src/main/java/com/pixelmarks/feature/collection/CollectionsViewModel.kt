package com.pixelmarks.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Collection
import com.pixelmarks.domain.usecase.CreateCollectionResult
import com.pixelmarks.domain.usecase.CreateCollectionUseCase
import com.pixelmarks.domain.usecase.DeleteCollectionUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.UpdateCollectionResult
import com.pixelmarks.domain.usecase.UpdateCollectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val bookmarkCounts: Map<Long, Int> = emptyMap(),
    val childCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CollectionsViewModel(
    private val observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val observeBookmarksUseCase: ObserveBookmarksUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase,
    private val updateCollectionUseCase: UpdateCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CollectionsUiState> = combine(
        observeCollectionsUseCase(),
        observeBookmarksUseCase(),
        _errorState
    ) { collections, bookmarks, error ->
        val bookmarkCounts = mutableMapOf<Long, Int>()
        for (bm in bookmarks) {
            val colId = bm.collectionId
            if (colId != null) {
                bookmarkCounts[colId] = (bookmarkCounts[colId] ?: 0) + 1
            }
        }

        val childCounts = mutableMapOf<Long, Int>()
        for (col in collections) {
            val pId = col.parentId
            if (pId != null) {
                childCounts[pId] = (childCounts[pId] ?: 0) + 1
            }
        }

        CollectionsUiState(
            collections = collections,
            bookmarkCounts = bookmarkCounts,
            childCounts = childCounts,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionsUiState(isLoading = true)
    )

    fun createCollection(
        name: String,
        parentId: Long? = null,
        icon: String? = null,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = createCollectionUseCase(name, parentId, icon, color)) {
                is CreateCollectionResult.Success -> {
                    _errorState.value = null
                    onSuccess()
                }
                is CreateCollectionResult.InvalidName -> {
                    _errorState.value = result.message
                }
                is CreateCollectionResult.DuplicateName -> {
                    _errorState.value = result.message
                }
                is CreateCollectionResult.Error -> {
                    _errorState.value = result.message
                }
            }
        }
    }

    fun updateCollection(
        collectionId: Long,
        newName: String,
        newParentId: Long? = null,
        icon: String? = null,
        color: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = updateCollectionUseCase(collectionId, newName, newParentId, icon, color)) {
                is UpdateCollectionResult.Success -> {
                    _errorState.value = null
                    onSuccess()
                }
                is UpdateCollectionResult.InvalidName -> {
                    _errorState.value = result.message
                }
                is UpdateCollectionResult.InvalidHierarchy -> {
                    _errorState.value = result.message
                }
                is UpdateCollectionResult.DuplicateName -> {
                    _errorState.value = result.message
                }
                is UpdateCollectionResult.Error -> {
                    _errorState.value = result.message
                }
            }
        }
    }

    fun deleteCollection(collectionId: Long) {
        viewModelScope.launch {
            val success = deleteCollectionUseCase(collectionId)
            if (!success) {
                _errorState.value = "Failed to delete collection"
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    companion object {
        fun provideFactory(
            observeCollectionsUseCase: ObserveCollectionsUseCase,
            observeBookmarksUseCase: ObserveBookmarksUseCase,
            createCollectionUseCase: CreateCollectionUseCase,
            updateCollectionUseCase: UpdateCollectionUseCase,
            deleteCollectionUseCase: DeleteCollectionUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CollectionsViewModel(
                    observeCollectionsUseCase,
                    observeBookmarksUseCase,
                    createCollectionUseCase,
                    updateCollectionUseCase,
                    deleteCollectionUseCase
                ) as T
            }
        }
    }
}
