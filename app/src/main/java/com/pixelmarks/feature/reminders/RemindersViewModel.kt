package com.pixelmarks.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Reminder
import com.pixelmarks.domain.usecase.CancelBookmarkReminderUseCase
import com.pixelmarks.domain.usecase.MarkReminderCompletedUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import com.pixelmarks.domain.usecase.ObserveUpcomingRemindersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderItem(
    val reminder: Reminder,
    val bookmark: Bookmark?
)

data class RemindersUiState(
    val reminderItems: List<ReminderItem> = emptyList(),
    val isLoading: Boolean = false
)

class RemindersViewModel(
    private val observeUpcomingRemindersUseCase: ObserveUpcomingRemindersUseCase,
    private val observeBookmarksUseCase: ObserveBookmarksUseCase,
    private val markReminderCompletedUseCase: MarkReminderCompletedUseCase,
    private val cancelBookmarkReminderUseCase: CancelBookmarkReminderUseCase
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = combine(
        observeUpcomingRemindersUseCase(),
        observeBookmarksUseCase()
    ) { reminders, bookmarks ->
        val bookmarkMap = bookmarks.associateBy { it.id }
        val items = reminders.map { reminder ->
            ReminderItem(
                reminder = reminder,
                bookmark = bookmarkMap[reminder.bookmarkId]
            )
        }
        RemindersUiState(reminderItems = items, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState(isLoading = true)
    )

    fun markCompleted(reminderId: Long) {
        viewModelScope.launch {
            markReminderCompletedUseCase(reminderId, isCompleted = true)
        }
    }

    fun cancelReminder(bookmarkId: Long) {
        viewModelScope.launch {
            cancelBookmarkReminderUseCase(bookmarkId)
        }
    }

    companion object {
        fun provideFactory(
            observeUpcomingRemindersUseCase: ObserveUpcomingRemindersUseCase,
            observeBookmarksUseCase: ObserveBookmarksUseCase,
            markReminderCompletedUseCase: MarkReminderCompletedUseCase,
            cancelBookmarkReminderUseCase: CancelBookmarkReminderUseCase
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RemindersViewModel(
                        observeUpcomingRemindersUseCase = observeUpcomingRemindersUseCase,
                        observeBookmarksUseCase = observeBookmarksUseCase,
                        markReminderCompletedUseCase = markReminderCompletedUseCase,
                        cancelBookmarkReminderUseCase = cancelBookmarkReminderUseCase
                    ) as T
                }
            }
    }
}
