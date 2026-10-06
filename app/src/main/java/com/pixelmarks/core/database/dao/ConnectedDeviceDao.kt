package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for paired PC client devices.
 */
@Dao
interface ConnectedDeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: ConnectedDeviceEntity): Long

    @Update
    suspend fun update(device: ConnectedDeviceEntity)

    @Delete
    suspend fun delete(device: ConnectedDeviceEntity)

    @Query("DELETE FROM connected_devices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM connected_devices WHERE id = :id")
    suspend fun getById(id: Long): ConnectedDeviceEntity?

    @Query("SELECT * FROM connected_devices WHERE tokenHash = :tokenHash LIMIT 1")
    suspend fun getByTokenHash(tokenHash: String): ConnectedDeviceEntity?

    @Query("SELECT * FROM connected_devices ORDER BY lastSeenAt DESC")
    fun observeAll(): Flow<List<ConnectedDeviceEntity>>

    @Query("SELECT * FROM connected_devices WHERE isBlocked = 0 AND revokedAt IS NULL ORDER BY lastSeenAt DESC")
    suspend fun getActiveDevices(): List<ConnectedDeviceEntity>
}
