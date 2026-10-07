package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelmarks.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for scheduled bookmark reminders.
 */
@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND reminderAt >= :currentTime ORDER BY reminderAt ASC")
    fun observeUpcoming(currentTime: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND reminderAt >= :currentTime ORDER BY reminderAt ASC")
    suspend fun getUpcoming(currentTime: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY reminderAt DESC")
    fun observeCompleted(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY reminderAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>
}
