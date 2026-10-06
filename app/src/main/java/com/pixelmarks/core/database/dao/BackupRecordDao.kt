package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelmarks.core.database.entity.BackupRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for local and remote backup archive records.
 */
@Dao
interface BackupRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BackupRecordEntity): Long

    @Update
    suspend fun update(record: BackupRecordEntity)

    @Delete
    suspend fun delete(record: BackupRecordEntity)

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM backup_records WHERE id = :id")
    suspend fun getById(id: Long): BackupRecordEntity?

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): BackupRecordEntity?
}
