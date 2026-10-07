package com.pixelmarks.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.Tag
import com.pixelmarks.core.security.PrivateModeManager
import com.pixelmarks.core.security.model.PrivacyMode
import com.pixelmarks.domain.usecase.ArchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.SearchBookmarksUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkFavoriteUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkPinUseCase
import com.pixelmarks.domain.usecase.TrashBookmarkUseCase
import com.pixelmarks.domain.usecase.UnarchiveBookmarkUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Immutable UI state for the Search and Filtering screen.
 */
data class SearchUiState(
    val query: String = "",
    val filters: BookmarkFilters = BookmarkFilters(),
    val sortOption: BookmarkSortOption = BookmarkSortOption.NEWEST,
    val bookmarks: List<Bookmark> = emptyList(),
    val availableCollections: List<Collection> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val totalBookmarksCount: Int = 0,
    val isLoading: Boolean = false,
    val isPrivateMode: Boolean = false
)

private data class SearchMetadata(
    val collections: List<Collection>,
    val tags: List<Tag>,
    val totalCount: Int
)

/**
 * ViewModel managing reactive search, debouncing, composable filtering, and deterministic sorting.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchBookmarksUseCase: SearchBookmarksUseCase,
    private val observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val observeTagsUseCase: ObserveTagsUseCase,
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase,
    private val toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase,
    private val toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase,
    private val archiveBookmarkUseCase: ArchiveBookmarkUseCase,
    private val unarchiveBookmarkUseCase: UnarchiveBookmarkUseCase,
    private val trashBookmarkUseCase: TrashBookmarkUseCase,
    private val privateModeManager: PrivateModeManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filters = MutableStateFlow(BookmarkFilters())
    val filters: StateFlow<BookmarkFilters> = _filters.asStateFlow()

    private val _sortOption = MutableStateFlow(BookmarkSortOption.NEWEST)
    val sortOption: StateFlow<BookmarkSortOption> = _sortOption.asStateFlow()

    private val isPrivateFlow: Flow<Boolean> = privateModeManager.mode.map { it == PrivacyMode.PRIVATE }

    // 250ms debounce on keystrokes, immediate 0ms reset when query is empty
    private val debouncedQuery: Flow<String> = _query
        .map { it.trim() }
        .debounce { q -> if (q.isEmpty()) 0L else 250L }
        .distinctUntilChanged()

    private val searchFlow: Flow<List<Bookmark>> = combine(
        debouncedQuery,
        _filters,
        _sortOption,
        isPrivateFlow
    ) { q, f, s, isPrivate ->
        searchBookmarksUseCase(query = q, filters = f, sort = s, isPrivate = isPrivate)
    }.flatMapLatest { it }

    private val metadataFlow: Flow<SearchMetadata> = isPrivateFlow.flatMapLatest { isPrivate ->
        combine(
            observeCollectionsUseCase(isPrivate = isPrivate),
            observeTagsUseCase(isPrivate = isPrivate),
            searchBookmarksUseCase.observeTotalCount(isPrivate = isPrivate)
        ) { collections, tags, totalCount ->
            SearchMetadata(collections, tags, totalCount)
        }
    }

    private data class SearchParams(
        val query: String,
        val filters: BookmarkFilters,
        val sortOption: BookmarkSortOption,
        val isPrivate: Boolean
    )

    private val searchParamsFlow = combine(_query, _filters, _sortOption, isPrivateFlow) { q, f, s, isPrivate ->
        SearchParams(q, f, s, isPrivate)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        searchParamsFlow,
        searchFlow,
        metadataFlow
    ) { params, bookmarks, metadata ->
        SearchUiState(
            query = params.query,
            filters = params.filters,
            sortOption = params.sortOption,
            bookmarks = bookmarks,
            availableCollections = metadata.collections,
            availableTags = metadata.tags,
            totalBookmarksCount = metadata.totalCount,
            isLoading = false,
            isPrivateMode = params.isPrivate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(isLoading = true)
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onClearQuery() {
        _query.value = ""
    }

    fun onToggleFavoriteFilter() {
        val current = _filters.value
        _filters.value = current.copy(favoriteOnly = !current.favoriteOnly)
    }

    fun onTogglePinnedFilter() {
        val current = _filters.value
        _filters.value = current.copy(pinnedOnly = !current.pinnedOnly)
    }

    fun onToggleArchivedFilter() {
        val current = _filters.value
        _filters.value = current.copy(archivedOnly = !current.archivedOnly)
    }

    fun onSelectCollection(collectionId: Long?) {
        val current = _filters.value
        _filters.value = current.copy(
            collectionId = if (current.collectionId == collectionId) null else collectionId
        )
    }

    fun onSelectTag(tagId: Long?) {
        val current = _filters.value
        _filters.value = current.copy(
            tagId = if (current.tagId == tagId) null else tagId
        )
    }

    fun onSelectSortOption(sortOption: BookmarkSortOption) {
        _sortOption.value = sortOption
    }

    fun onResetFilters() {
        _filters.value = BookmarkFilters()
    }

    fun toggleFavorite(bookmark: Bookmark) {
        viewModelScope.launch {
            toggleBookmarkFavoriteUseCase(bookmark)
        }
    }

    fun togglePin(bookmark: Bookmark) {
        viewModelScope.launch {
            toggleBookmarkPinUseCase(bookmark)
        }
    }

    fun archiveBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            archiveBookmarkUseCase(bookmark)
        }
    }

    fun unarchiveBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            unarchiveBookmarkUseCase(bookmark)
        }
    }

    fun moveToTrash(bookmark: Bookmark) {
        viewModelScope.launch {
            trashBookmarkUseCase(bookmark)
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            deleteBookmarkUseCase(bookmarkId)
        }
    }

    companion object {
        fun provideFactory(
            searchBookmarksUseCase: SearchBookmarksUseCase,
            observeCollectionsUseCase: ObserveCollectionsUseCase,
            observeTagsUseCase: ObserveTagsUseCase,
            deleteBookmarkUseCase: DeleteBookmarkUseCase,
            toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase,
            toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase,
            archiveBookmarkUseCase: ArchiveBookmarkUseCase,
            unarchiveBookmarkUseCase: UnarchiveBookmarkUseCase,
            trashBookmarkUseCase: TrashBookmarkUseCase,
            privateModeManager: PrivateModeManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    searchBookmarksUseCase,
                    observeCollectionsUseCase,
                    observeTagsUseCase,
                    deleteBookmarkUseCase,
                    toggleBookmarkFavoriteUseCase,
                    toggleBookmarkPinUseCase,
                    archiveBookmarkUseCase,
                    unarchiveBookmarkUseCase,
                    trashBookmarkUseCase,
                    privateModeManager
                ) as T
            }
        }
    }
}
