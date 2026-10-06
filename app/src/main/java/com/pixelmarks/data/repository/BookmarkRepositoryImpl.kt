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

    override fun observeAll(): Flow<List<Bookmark>> =
        bookmarkDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByCollection(collectionId: Long): Flow<List<Bookmark>> =
        bookmarkDao.observeByCollection(collectionId).map { list -> list.map { it.toDomain() } }

    override fun observeUnsorted(): Flow<List<Bookmark>> =
        bookmarkDao.observeUnsorted().map { list -> list.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<Bookmark>> =
        bookmarkDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override fun observePinned(): Flow<List<Bookmark>> =
        bookmarkDao.observePinned().map { list -> list.map { it.toDomain() } }

    override fun observeArchived(): Flow<List<Bookmark>> =
        bookmarkDao.observeArchived().map { list -> list.map { it.toDomain() } }

    override fun observeByDomain(domain: String): Flow<List<Bookmark>> =
        bookmarkDao.observeByDomain(domain).map { list -> list.map { it.toDomain() } }

    override fun observeTotalCount(): Flow<Int> =
        bookmarkDao.observeCount()

    override fun observeBookmarks(
        query: String,
        filters: BookmarkFilters,
        sort: BookmarkSortOption
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
            tagId = filters.tagId
        ).map { entities ->
            entities.map { it.toDomain() }.sortedWith(sort.comparator)
        }
    }

    override suspend fun getById(id: Long): Bookmark? =
        bookmarkDao.getById(id)?.toDomain()

    override suspend fun findByNormalizedUrl(normalizedUrl: String): List<Bookmark> =
        bookmarkDao.getByNormalizedUrl(normalizedUrl).map { it.toDomain() }

    override suspend fun insert(bookmark: Bookmark): Long =
        bookmarkDao.insert(bookmark.toEntity())

    override suspend fun update(bookmark: Bookmark) =
        bookmarkDao.update(bookmark.toEntity())

    override suspend fun delete(bookmark: Bookmark) =
        bookmarkDao.delete(bookmark.toEntity())

    override suspend fun deleteById(id: Long) =
        bookmarkDao.deleteById(id)
}
