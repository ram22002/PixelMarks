package com.pixelmarks.domain.usecase

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to search and filter bookmarks reactively with composable filters and deterministic sorting.
 * Observes the local Room database as the single source of truth.
 */
class SearchBookmarksUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(
        query: String = "",
        filters: BookmarkFilters = BookmarkFilters(),
        sort: BookmarkSortOption = BookmarkSortOption.NEWEST,
        isPrivate: Boolean = false
    ): Flow<List<Bookmark>> = bookmarkRepository.observeBookmarks(
        query = query,
        filters = filters,
        sort = sort,
        isPrivate = isPrivate
    )

    fun observeTotalCount(isPrivate: Boolean = false): Flow<Int> = bookmarkRepository.observeTotalCount(isPrivate)
}
