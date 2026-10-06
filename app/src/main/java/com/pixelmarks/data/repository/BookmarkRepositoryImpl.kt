package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.model.Bookmark
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
