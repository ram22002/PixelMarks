package com.pixelmarks.data.repository

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Bookmark domain operations.
 * Acts as the boundary between the domain layer and Room persistence.
 */
interface BookmarkRepository {
    fun observeAll(isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeById(id: Long): Flow<Bookmark?>
    fun observeByCollection(collectionId: Long, isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeUnsorted(isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeFavorites(isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observePinned(isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeArchived(isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeTrashed(isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeByDomain(domain: String, isPrivate: Boolean = false): Flow<List<Bookmark>>
    fun observeTotalCount(isPrivate: Boolean = false): Flow<Int>
    fun observeTrashedCount(isPrivate: Boolean = false): Flow<Int>
    fun observeBookmarks(
        query: String = "",
        filters: BookmarkFilters = BookmarkFilters(),
        sort: BookmarkSortOption = BookmarkSortOption.NEWEST,
        isPrivate: Boolean = false
    ): Flow<List<Bookmark>>
    suspend fun getById(id: Long): Bookmark?
    suspend fun findByNormalizedUrl(normalizedUrl: String, isPrivate: Boolean = false): List<Bookmark>
    suspend fun getAllByPrivacy(isPrivate: Boolean): List<Bookmark>
    suspend fun insert(bookmark: Bookmark): Long
    suspend fun update(bookmark: Bookmark)
    suspend fun delete(bookmark: Bookmark)
    suspend fun deleteById(id: Long)
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun setPinned(id: Long, isPinned: Boolean)
    suspend fun setArchived(id: Long, isArchived: Boolean)
    suspend fun moveToTrash(id: Long)
    suspend fun restoreFromTrash(id: Long)
    suspend fun permanentlyDelete(id: Long)
    suspend fun recordVisit(id: Long, openedAt: Long = System.currentTimeMillis())
}
