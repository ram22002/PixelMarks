package com.pixelmarks.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderSchedulerTest {

    @Test
    fun reminderScheduler_constantsAreCorrect() {
        assertEquals("bookmark_reminders_v2", ReminderScheduler.CHANNEL_ID)
        assertEquals("extra_reminder_id", ReminderScheduler.EXTRA_REMINDER_ID)
        assertEquals("extra_bookmark_id", ReminderScheduler.EXTRA_BOOKMARK_ID)
        assertEquals("extra_title", ReminderScheduler.EXTRA_TITLE)
        assertEquals("extra_url", ReminderScheduler.EXTRA_URL)
    }

    @Test
    fun bootReceiver_canBeInstantiated() {
        val receiver = BootReceiver()
        assertNotNull(receiver)
    }

    @Test
    fun reminderAlarmReceiver_canBeInstantiated() {
        val receiver = ReminderAlarmReceiver()
        assertNotNull(receiver)
    }
}
