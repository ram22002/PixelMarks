package com.pixelmarks.data.repository

import com.pixelmarks.core.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing scheduled read-later reminders.
 */
interface ReminderRepository {
    fun observeUpcoming(currentTime: Long = System.currentTimeMillis()): Flow<List<Reminder>>
    fun observeAll(): Flow<List<Reminder>>
    suspend fun getById(id: Long): Reminder?
    suspend fun getByBookmarkId(bookmarkId: Long): Reminder?
    suspend fun setReminder(bookmarkId: Long, reminderAt: Long): Long
    suspend fun cancelReminder(bookmarkId: Long)
    suspend fun deleteById(id: Long)
    suspend fun markCompleted(id: Long, isCompleted: Boolean = true)
}
