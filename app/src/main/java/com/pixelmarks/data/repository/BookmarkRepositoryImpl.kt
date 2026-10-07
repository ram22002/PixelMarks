package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.core.util.SearchQueryUtils
import com.pixelmarks.data.mapper.toDomain
import com.pixelmarks.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of BookmarkRepository backed by Room BookmarkDao.
 */
class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun observeAll(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeAll(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeByCollection(collectionId: Long, isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeByCollection(collectionId, isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeUnsorted(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeUnsorted(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Bookmark?> =
        bookmarkDao.observeById(id).map { it?.toDomain() }

    override fun observeFavorites(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeFavorites(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observePinned(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observePinned(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeArchived(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeArchived(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeTrashed(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeTrashed(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeByDomain(domain: String, isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeByDomain(domain, isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeTotalCount(isPrivate: Boolean): Flow<Int> =
        bookmarkDao.observeCount(isPrivate)

    override fun observeTrashedCount(isPrivate: Boolean): Flow<Int> =
        bookmarkDao.observeTrashedCount(isPrivate)

    override fun observeBookmarks(
        query: String,
        filters: BookmarkFilters,
        sort: BookmarkSortOption,
        isPrivate: Boolean
    ): Flow<List<Bookmark>> {
        val trimmedQuery = query.trim()
        val escapedQuery = SearchQueryUtils.escapeSqlLike(trimmedQuery)
        return bookmarkDao.searchBookmarks(
            query = trimmedQuery,
            escapedQuery = escapedQuery,
            favoriteOnly = filters.favoriteOnly,
            pinnedOnly = filters.pinnedOnly,
            archivedOnly = filters.archivedOnly,
            collectionId = filters.collectionId,
            tagId = filters.tagId,
            isPrivate = isPrivate
        ).map { entities ->
            entities.map { it.toDomain() }.sortedWith(sort.comparator)
        }
    }

    override suspend fun getById(id: Long): Bookmark? =
        bookmarkDao.getById(id)?.toDomain()

    override suspend fun findByNormalizedUrl(normalizedUrl: String, isPrivate: Boolean): List<Bookmark> =
        bookmarkDao.getByNormalizedUrlAndPrivacy(normalizedUrl, isPrivate).map { it.toDomain() }

    override suspend fun getAllByPrivacy(isPrivate: Boolean): List<Bookmark> =
        bookmarkDao.getAllByPrivacy(isPrivate).map { it.toDomain() }

    override suspend fun insert(bookmark: Bookmark): Long =
        bookmarkDao.insert(bookmark.toEntity())

    override suspend fun update(bookmark: Bookmark) =
        bookmarkDao.update(bookmark.toEntity())

    override suspend fun delete(bookmark: Bookmark) =
        bookmarkDao.delete(bookmark.toEntity())

    override suspend fun deleteById(id: Long) =
        bookmarkDao.deleteById(id)

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        bookmarkDao.setFavorite(id, isFavorite)

    override suspend fun setPinned(id: Long, isPinned: Boolean) =
        bookmarkDao.setPinned(id, isPinned)

    override suspend fun setArchived(id: Long, isArchived: Boolean) =
        bookmarkDao.setArchived(id, isArchived)

    override suspend fun moveToTrash(id: Long) =
        bookmarkDao.moveToTrash(id)

    override suspend fun restoreFromTrash(id: Long) =
        bookmarkDao.restoreFromTrash(id)

    override suspend fun permanentlyDelete(id: Long) =
        bookmarkDao.deleteById(id)

    override suspend fun recordVisit(id: Long, openedAt: Long) =
        bookmarkDao.incrementVisitCount(id, openedAt)
}
