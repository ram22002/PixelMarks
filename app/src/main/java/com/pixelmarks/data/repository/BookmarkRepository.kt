package com.pixelmarks.data.repository

import com.pixelmarks.core.model.Bookmark
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Bookmark domain operations.
 * Acts as the boundary between the domain layer and Room persistence.
 */
interface BookmarkRepository {
    fun observeAll(): Flow<List<Bookmark>>
    fun observeByCollection(collectionId: Long): Flow<List<Bookmark>>
    fun observeUnsorted(): Flow<List<Bookmark>>
    fun observeFavorites(): Flow<List<Bookmark>>
    fun observePinned(): Flow<List<Bookmark>>
    fun observeArchived(): Flow<List<Bookmark>>
    fun observeByDomain(domain: String): Flow<List<Bookmark>>
    suspend fun getById(id: Long): Bookmark?
    suspend fun findByNormalizedUrl(normalizedUrl: String): List<Bookmark>
    suspend fun insert(bookmark: Bookmark): Long
    suspend fun update(bookmark: Bookmark)
    suspend fun delete(bookmark: Bookmark)
    suspend fun deleteById(id: Long)
}
