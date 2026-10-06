package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelmarks.core.database.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Collection hierarchy operations.
 */
@Dao
interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionEntity>): List<Long>

    @Update
    suspend fun update(collection: CollectionEntity)

    @Delete
    suspend fun delete(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: Long): CollectionEntity?

    @Query("SELECT * FROM collections WHERE isDomain = 1 AND domain = :domain LIMIT 1")
    suspend fun getByDomain(domain: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE parentId IS NULL ORDER BY position ASC, name ASC")
    fun observeRootCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE parentId = :parentId ORDER BY position ASC, name ASC")
    fun observeChildren(parentId: Long): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY position ASC, name ASC")
    fun observeAll(): Flow<List<CollectionEntity>>
}
