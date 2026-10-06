package com.pixelmarks.domain.usecase

import com.pixelmarks.core.model.Reminder
import com.pixelmarks.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

class SetBookmarkReminderUseCase(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(bookmarkId: Long, reminderAt: Long): Long =
        reminderRepository.setReminder(bookmarkId, reminderAt)
}

class CancelBookmarkReminderUseCase(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(bookmarkId: Long) =
        reminderRepository.cancelReminder(bookmarkId)
}

class GetBookmarkReminderUseCase(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(bookmarkId: Long): Reminder? =
        reminderRepository.getByBookmarkId(bookmarkId)
}

class ObserveUpcomingRemindersUseCase(
    private val reminderRepository: ReminderRepository
) {
    operator fun invoke(currentTime: Long = System.currentTimeMillis()): Flow<List<Reminder>> =
        reminderRepository.observeUpcoming(currentTime)
}

class MarkReminderCompletedUseCase(
    private val reminderRepository: ReminderRepository
) {
    suspend operator fun invoke(reminderId: Long, isCompleted: Boolean = true) =
        reminderRepository.markCompleted(reminderId, isCompleted)
}
