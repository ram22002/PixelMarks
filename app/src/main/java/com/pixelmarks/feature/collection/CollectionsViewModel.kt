package com.pixelmarks.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.security.PrivateModeManager
import com.pixelmarks.core.security.model.PrivacyMode
import com.pixelmarks.domain.usecase.CreateCollectionResult
import com.pixelmarks.domain.usecase.CreateCollectionUseCase
import com.pixelmarks.domain.usecase.DeleteCollectionUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.UpdateCollectionResult
import com.pixelmarks.domain.usecase.UpdateCollectionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val bookmarkCounts: Map<Long, Int> = emptyMap(),
    val childCounts: Map<Long, Int> = emptyMap(),
    val collectionFavicons: Map<Long, List<String>> = emptyMap(),
    val isLoading: Boolean = false,
    val isPrivateMode: Boolean = false,
    val errorMessage: String? = null
)

class CollectionsViewModel(
    private val observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val observeBookmarksUseCase: ObserveBookmarksUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase,
    private val updateCollectionUseCase: UpdateCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
    private val privateModeManager: PrivateModeManager
) : ViewModel() {

    private val _errorState = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CollectionsUiState> = privateModeManager.mode
        .flatMapLatest { mode ->
            val isPrivate = mode == PrivacyMode.PRIVATE
            combine(
                observeCollectionsUseCase(isPrivate = isPrivate),
                observeBookmarksUseCase(isPrivate = isPrivate),
                _errorState
            ) { collections, bookmarks, error ->
                val bookmarkCounts = mutableMapOf<Long, Int>()
                val collectionFavicons = mutableMapOf<Long, MutableList<String>>()
                for (bm in bookmarks) {
                    val colId = bm.collectionId
                    if (colId != null) {
                        bookmarkCounts[colId] = (bookmarkCounts[colId] ?: 0) + 1
                        val fav = bm.faviconUrl
                        if (!fav.isNullOrBlank()) {
                            val list = collectionFavicons.getOrPut(colId) { mutableListOf() }
                            if (list.size < 4 && !list.contains(fav)) {
                                list.add(fav)
                            }
                        }
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
                    collectionFavicons = collectionFavicons,
                    isLoading = false,
                    isPrivateMode = isPrivate,
                    errorMessage = error
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CollectionsUiState(isLoading = true)
        )

    fun createCollection(
        name: String,
        parentId: Long? = null,
        icon: String? = null,
        color: String? = null,
        isLocked: Boolean = false,
        isHidden: Boolean = false,
        description: String? = null,
        keywords: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val isPrivate = privateModeManager.isPrivateActive()
        viewModelScope.launch {
            when (val result = createCollectionUseCase(
                name = name,
                parentId = parentId,
                icon = icon,
                color = color,
                isLocked = isLocked,
                isHidden = isHidden,
                isPrivate = isPrivate,
                description = description,
                keywords = keywords
            )) {
                is CreateCollectionResult.Success -> {
                    _errorState.value = null
                    onSuccess()
                }
                is CreateCollectionResult.InvalidName -> {
                    _errorState.value = result.message
                }
                is CreateCollectionResult.InvalidHierarchy -> {
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
        isLocked: Boolean? = null,
        isHidden: Boolean? = null,
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
                isLocked = isLocked,
                isHidden = isHidden,
                description = description,
                keywords = keywords
            )) {
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
            deleteCollectionUseCase: DeleteCollectionUseCase,
            privateModeManager: PrivateModeManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CollectionsViewModel(
                    observeCollectionsUseCase,
                    observeBookmarksUseCase,
                    createCollectionUseCase,
                    updateCollectionUseCase,
                    deleteCollectionUseCase,
                    privateModeManager
                ) as T
            }
        }
    }
}
