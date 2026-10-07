package com.pixelmarks.core.reminder

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderPrivacyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun buildNotification_nonSensitive_includesTitleAndUrl() {
        val title = "Public Kotlin Article"
        val url = "https://kotlinlang.org/docs"

        val builder = ReminderScheduler.buildNotification(
            context = context,
            reminderId = 1L,
            bookmarkId = 100L,
            title = title,
            url = url,
            isSensitive = false
        )

        val notification = builder.build()
        assertEquals(title, notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString())
        assertEquals("Time to read: $url", notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString())
        assertEquals(NotificationCompat.VISIBILITY_PUBLIC, notification.visibility)
    }

    @Test
    fun buildNotification_sensitive_protectsTitleAndUrl() {
        val sensitiveTitle = "Confidential Bank Statement"
        val sensitiveUrl = "https://mybank.com/private/statement.pdf"

        val builder = ReminderScheduler.buildNotification(
            context = context,
            reminderId = 2L,
            bookmarkId = 200L,
            title = sensitiveTitle,
            url = sensitiveUrl,
            isSensitive = true
        )

        val notification = builder.build()
        // Must NOT match sensitive title or sensitive url
        assertNotEquals(sensitiveTitle, notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString())
        assertNotEquals(sensitiveUrl, notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString())

        // Must display privacy-safe fallback
        assertEquals("Bookmark reminder", notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString())
        assertEquals("You have a bookmark reminder.", notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString())
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertNotNull(notification.publicVersion)
    }
}
