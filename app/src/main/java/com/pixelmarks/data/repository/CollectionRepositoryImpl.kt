package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.CollectionDao
import com.pixelmarks.core.model.Collection
import com.pixelmarks.data.mapper.toDomain
import com.pixelmarks.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of CollectionRepository backed by Room CollectionDao.
 */
class CollectionRepositoryImpl(
    private val collectionDao: CollectionDao
) : CollectionRepository {

    override fun observeRootCollections(): Flow<List<Collection>> =
        collectionDao.observeRootCollections().map { list -> list.map { it.toDomain() } }

    override fun observeChildren(parentId: Long): Flow<List<Collection>> =
        collectionDao.observeChildren(parentId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Collection>> =
        collectionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Collection? =
        collectionDao.getById(id)?.toDomain()

    override suspend fun getByDomain(domain: String): Collection? =
        collectionDao.getByDomain(domain)?.toDomain()

    override suspend fun insert(collection: Collection): Long =
        collectionDao.insert(collection.toEntity())

    override suspend fun update(collection: Collection) =
        collectionDao.update(collection.toEntity())

    override suspend fun delete(collection: Collection) =
        collectionDao.delete(collection.toEntity())

    override suspend fun deleteById(id: Long) =
        collectionDao.deleteById(id)
}
