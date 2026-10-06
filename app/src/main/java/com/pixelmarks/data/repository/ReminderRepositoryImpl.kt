package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.ReminderDao
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.model.Reminder
import com.pixelmarks.data.mapper.toDomain
import com.pixelmarks.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of ReminderRepository backed by Room ReminderDao and BookmarkRepository.
 */
class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val bookmarkRepository: BookmarkRepository
) : ReminderRepository {

    override fun observeUpcoming(currentTime: Long): Flow<List<Reminder>> =
        reminderDao.observeUpcoming(currentTime).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Reminder? =
        reminderDao.getById(id)?.toDomain()

    override suspend fun getByBookmarkId(bookmarkId: Long): Reminder? =
        reminderDao.getByBookmarkId(bookmarkId)?.toDomain()

    override suspend fun setReminder(bookmarkId: Long, reminderAt: Long): Long {
        val now = System.currentTimeMillis()
        val existing = reminderDao.getByBookmarkId(bookmarkId)
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

        // Sync with bookmark entity
        val bookmark = bookmarkRepository.getById(bookmarkId)
        if (bookmark != null) {
            bookmarkRepository.update(bookmark.copy(reminderAt = reminderAt, updatedAt = now))
        }

        return reminderId
    }

    override suspend fun cancelReminder(bookmarkId: Long) {
        val existing = reminderDao.getByBookmarkId(bookmarkId)
        if (existing != null) {
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
            reminderDao.deleteById(id)
            val bookmark = bookmarkRepository.getById(existing.bookmarkId)
            if (bookmark != null) {
                bookmarkRepository.update(bookmark.copy(reminderAt = null, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    override suspend fun markCompleted(id: Long, isCompleted: Boolean) {
        val existing = reminderDao.getById(id) ?: return
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
