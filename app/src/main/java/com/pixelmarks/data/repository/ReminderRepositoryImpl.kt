package com.pixelmarks.data.repository

import android.content.Context
import com.pixelmarks.core.database.dao.ReminderDao
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.model.Reminder
import com.pixelmarks.core.reminder.ReminderScheduler
import com.pixelmarks.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of ReminderRepository backed by Room ReminderDao, BookmarkRepository,
 * and Android AlarmManager synchronization via ReminderScheduler.
 */
class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val bookmarkRepository: BookmarkRepository,
    private val context: Context? = null
) : ReminderRepository {

    override fun observeUpcoming(currentTime: Long): Flow<List<Reminder>> =
        reminderDao.observeUpcoming(currentTime).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByBookmarkId(bookmarkId: Long): Flow<Reminder?> =
        reminderDao.observeByBookmarkId(bookmarkId).map { it?.toDomain() }

    override suspend fun getById(id: Long): Reminder? =
        reminderDao.getById(id)?.toDomain()

    override suspend fun getByBookmarkId(bookmarkId: Long): Reminder? =
        reminderDao.getByBookmarkId(bookmarkId)?.toDomain()

    override suspend fun setReminder(bookmarkId: Long, reminderAt: Long): Long {
        val now = System.currentTimeMillis()
        val existing = reminderDao.getByBookmarkId(bookmarkId)

        // Cancel previous alarm if updating
        if (existing != null && context != null) {
            ReminderScheduler.cancelReminder(context, existing.id)
        }

        val reminderId = if (existing != null) {
            val updated = existing.copy(
                reminderAt = reminderAt,
                updatedAt = now,
                isCompleted = false
            )
            reminderDao.update(updated)
            existing.id
        } else {
            val entity = ReminderEntity(
                bookmarkId = bookmarkId,
                reminderAt = reminderAt,
                createdAt = now,
                updatedAt = now,
                isCompleted = false
            )
            reminderDao.insert(entity)
        }

        // Sync with bookmark entity in Room
        val bookmark = bookmarkRepository.getById(bookmarkId)
        if (bookmark != null) {
            bookmarkRepository.update(bookmark.copy(reminderAt = reminderAt, updatedAt = now))
            // Schedule AlarmManager alarm
            if (context != null) {
                ReminderScheduler.scheduleReminder(
                    context = context,
                    reminderId = reminderId,
                    bookmarkId = bookmarkId,
                    title = bookmark.title,
                    url = bookmark.url,
                    triggerAtMillis = reminderAt
                )
            }
        }

        return reminderId
    }

    override suspend fun cancelReminder(bookmarkId: Long) {
        val existing = reminderDao.getByBookmarkId(bookmarkId)
        if (existing != null) {
            if (context != null) {
                ReminderScheduler.cancelReminder(context, existing.id)
            }
            reminderDao.delete(existing)
        }
        val bookmark = bookmarkRepository.getById(bookmarkId)
        if (bookmark != null) {
            bookmarkRepository.update(bookmark.copy(reminderAt = null, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteById(id: Long) {
        val existing = reminderDao.getById(id)
        if (existing != null) {
            if (context != null) {
                ReminderScheduler.cancelReminder(context, id)
            }
            reminderDao.deleteById(id)
            val bookmark = bookmarkRepository.getById(existing.bookmarkId)
            if (bookmark != null) {
                bookmarkRepository.update(bookmark.copy(reminderAt = null, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun markCompleted(id: Long, isCompleted: Boolean) {
        val existing = reminderDao.getById(id) ?: return
        if (context != null) {
            ReminderScheduler.cancelReminder(context, id)
        }
        val updated = existing.copy(
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        reminderDao.update(updated)
        if (isCompleted) {
            val bookmark = bookmarkRepository.getById(existing.bookmarkId)
            if (bookmark != null) {
                bookmarkRepository.update(bookmark.copy(reminderAt = null, updatedAt = System.currentTimeMillis()))
            }
        }
    }
}
