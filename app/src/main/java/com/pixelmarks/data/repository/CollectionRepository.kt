package com.pixelmarks.data.repository

import com.pixelmarks.core.model.Collection
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Collection domain operations.
 */
interface CollectionRepository {
    fun observeRootCollections(): Flow<List<Collection>>
    fun observeChildren(parentId: Long): Flow<List<Collection>>
    fun observeAll(): Flow<List<Collection>>
    suspend fun getById(id: Long): Collection?
    suspend fun getByDomain(domain: String): Collection?
    suspend fun insert(collection: Collection): Long
    suspend fun update(collection: Collection)
    suspend fun delete(collection: Collection)
    suspend fun deleteById(id: Long)
}
