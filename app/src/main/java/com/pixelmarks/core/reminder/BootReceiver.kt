package com.pixelmarks.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pixelmarks.app.PixelMarksApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered on system reboot (BOOT_COMPLETED) or app update (MY_PACKAGE_REPLACED)
 * to reschedule all pending bookmark reminders with AlarmManager.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val appContext = context.applicationContext as? PixelMarksApplication ?: return
        ReminderScheduler.createNotificationChannel(appContext)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val upcoming = appContext.database.reminderDao().getUpcoming(now)
                for (reminder in upcoming) {
                    val bookmark = appContext.database.bookmarkDao().getById(reminder.bookmarkId)
                    if (bookmark != null) {
                        ReminderScheduler.scheduleReminder(
                            context = appContext,
                            reminderId = reminder.id,
                            bookmarkId = bookmark.id,
                            title = bookmark.title,
                            url = bookmark.url,
                            triggerAtMillis = reminder.reminderAt
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore errors during boot rescheduling
            } finally {
                pendingResult.finish()
            }
        }
    }
}
