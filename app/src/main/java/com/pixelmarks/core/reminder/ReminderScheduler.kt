package com.pixelmarks.core.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelmarks.app.MainActivity
import com.pixelmarks.app.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

object ReminderScheduler {

    const val CHANNEL_ID = "bookmark_reminders_v2"
    private const val OLD_CHANNEL_ID = "bookmark_reminders_channel"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_BOOKMARK_ID = "extra_bookmark_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_URL = "extra_url"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Delete old channel if it was created with lower importance
            try {
                notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
            } catch (_: Exception) {
                // Ignore cleanup error
            }

            val name = "Bookmark Reminders"
            val descriptionText = "Notifications for scheduled read-later bookmarks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(
        context: Context,
        reminderId: Long,
        bookmarkId: Long,
        title: String,
        url: String,
        triggerAtMillis: Long
    ) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_URL, url)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
    fun buildNotification(
        context: Context,
        reminderId: Long,
        bookmarkId: Long,
        title: String,
        url: String,
        isSensitive: Boolean
    ): NotificationCompat.Builder {
        val displayTitle = if (isSensitive) "Bookmark reminder" else title
        val displayText = if (isSensitive) {
            "You have a bookmark reminder."
        } else {
            if (url.isNotBlank()) "Time to read: $url" else "Scheduled read-later reminder"
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("bookmarkId", bookmarkId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            bookmarkId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Bookmark reminder")
            .setContentText("You have a bookmark reminder.")
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(if (isSensitive) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .setPublicVersion(publicNotification)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createNotificationChannel(context)

        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0L)
        val bookmarkId = intent.getLongExtra(ReminderScheduler.EXTRA_BOOKMARK_ID, 0L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Bookmark Reminder"
        val url = intent.getStringExtra(ReminderScheduler.EXTRA_URL) ?: ""

        val app = context.applicationContext as? com.pixelmarks.app.PixelMarksApplication
        val appContainer = app?.container
        var isSensitive = false

        if (appContainer != null) {
            try {
                runBlocking {
                    val appLockEnabled = withTimeoutOrNull(1000L) {
                        appContainer.securityPreferencesRepository.observeAppLockEnabled().first()
                    } ?: false
                    val bookmark = kotlinx.coroutines.withTimeoutOrNull(1000L) {
                        appContainer.bookmarkRepository.getById(bookmarkId)
                    }
                    val isCollectionProtected = if (bookmark?.collectionId != null) {
                        val col = appContainer.collectionRepository.getById(bookmark.collectionId)
                        col?.isLocked == true || col?.isHidden == true || col?.isPrivate == true
                    } else false

                    if (appLockEnabled || isCollectionProtected || bookmark?.isPrivate == true) {
                        isSensitive = true
                    }
                }
            } catch (_: Exception) {
                // Ignore fallback to safe default
            }
        }

        val notificationBuilder = ReminderScheduler.buildNotification(
            context = context,
            reminderId = reminderId,
            bookmarkId = bookmarkId,
            title = title,
            url = url,
            isSensitive = isSensitive
        )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(reminderId.toInt(), notificationBuilder.build())
            }
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS runtime permission denied
        }
    }
}
