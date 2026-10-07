package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelmarks.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tag management.
 */
@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>): List<Long>

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name AND isPrivate = :isPrivate LIMIT 1")
    suspend fun getByName(name: String, isPrivate: Boolean = false): TagEntity?

    @Query("SELECT * FROM tags WHERE isPrivate = :isPrivate ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(isPrivate: Boolean = false): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE isPrivate = :isPrivate ORDER BY id ASC")
    suspend fun getAllByPrivacy(isPrivate: Boolean): List<TagEntity>

    @Query("SELECT * FROM tags ORDER BY id ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
