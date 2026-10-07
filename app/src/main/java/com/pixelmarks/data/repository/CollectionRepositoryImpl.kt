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

    override fun observeRootCollections(isPrivate: Boolean): Flow<List<Collection>> =
        collectionDao.observeRootCollections(isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeChildren(parentId: Long, isPrivate: Boolean): Flow<List<Collection>> =
        collectionDao.observeChildren(parentId, isPrivate).map { list -> list.map { it.toDomain() } }

    override fun observeAll(isPrivate: Boolean): Flow<List<Collection>> =
        collectionDao.observeAll(isPrivate).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllByPrivacy(isPrivate: Boolean): List<Collection> =
        collectionDao.getAllByPrivacy(isPrivate).map { it.toDomain() }

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
