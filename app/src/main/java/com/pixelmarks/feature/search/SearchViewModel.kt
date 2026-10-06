package com.pixelmarks.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.Tag
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.SearchBookmarksUseCase
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
    val isLoading: Boolean = false
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
    private val deleteBookmarkUseCase: DeleteBookmarkUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filters = MutableStateFlow(BookmarkFilters())
    val filters: StateFlow<BookmarkFilters> = _filters.asStateFlow()

    private val _sortOption = MutableStateFlow(BookmarkSortOption.NEWEST)
    val sortOption: StateFlow<BookmarkSortOption> = _sortOption.asStateFlow()

    // 250ms debounce on keystrokes, immediate 0ms reset when query is empty
    private val debouncedQuery: Flow<String> = _query
        .map { it.trim() }
        .debounce { q -> if (q.isEmpty()) 0L else 250L }
        .distinctUntilChanged()

    private val searchFlow: Flow<List<Bookmark>> = combine(
        debouncedQuery,
        _filters,
        _sortOption
    ) { q, f, s ->
        Triple(q, f, s)
    }.flatMapLatest { (q, f, s) ->
        searchBookmarksUseCase(query = q, filters = f, sort = s)
    }

    private val metadataFlow: Flow<SearchMetadata> = combine(
        observeCollectionsUseCase(),
        observeTagsUseCase(),
        searchBookmarksUseCase.observeTotalCount()
    ) { collections, tags, totalCount ->
        SearchMetadata(collections, tags, totalCount)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _filters,
        _sortOption,
        searchFlow,
        metadataFlow
    ) { q, f, s, bookmarks, metadata ->
        SearchUiState(
            query = q,
            filters = f,
            sortOption = s,
            bookmarks = bookmarks,
            availableCollections = metadata.collections,
            availableTags = metadata.tags,
            totalBookmarksCount = metadata.totalCount,
            isLoading = false
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
            deleteBookmarkUseCase: DeleteBookmarkUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    searchBookmarksUseCase,
                    observeCollectionsUseCase,
                    observeTagsUseCase,
                    deleteBookmarkUseCase
                ) as T
            }
        }
    }
}
