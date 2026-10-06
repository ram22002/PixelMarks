package com.pixelmarks.core.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderTimeHelperTest {

    @Test
    fun testCustomFutureDateTime() {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.OCTOBER, 20, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateUtc = utcCal.timeInMillis

        val combined = ReminderTimeHelper.combineDateAndTime(
            dateUtcMidnightMillis = dateUtc,
            hourOfDay = 19,
            minute = 30,
            timeZone = TimeZone.getTimeZone("UTC")
        )

        val resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = combined
        }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, resultCal.get(Calendar.MONTH))
        assertEquals(20, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, resultCal.get(Calendar.MINUTE))
    }

    @Test
    fun testCustomTodayWithFutureTime() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis

        // Future time today at 14:00 (2:00 PM)
        val futureTimeToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val validation = ReminderTimeHelper.validateReminderTime(futureTimeToday, now)
        assertTrue(validation is ReminderTimeHelper.ValidationResult.Valid)
    }

    @Test
    fun testCustomTodayWithPastTimeRejected() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis

        // Past time today at 10:00 AM
        val pastTimeToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val validation = ReminderTimeHelper.validateReminderTime(pastTimeToday, now)
        assertTrue(validation is ReminderTimeHelper.ValidationResult.Invalid)
        assertEquals("Please select a time in the future", (validation as ReminderTimeHelper.ValidationResult.Invalid).message)
    }

    @Test
    fun testPastDateRejected() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis

        // Yesterday UTC midnight
        val yesterdayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.OCTOBER, 9, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val isSelectable = ReminderTimeHelper.isDateSelectable(yesterdayUtc, now)
        assertFalse(isSelectable)
    }

    @Test
    fun testTodayDateIsSelectable() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis

        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.OCTOBER, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val isSelectable = ReminderTimeHelper.isDateSelectable(todayUtc, now)
        assertTrue(isSelectable)
    }

    @Test
    fun testTimezoneConversion() {
        val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.DECEMBER, 25, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tzNewYork = TimeZone.getTimeZone("America/New_York")
        val tzTokyo = TimeZone.getTimeZone("Asia/Tokyo")

        val nyMillis = ReminderTimeHelper.combineDateAndTime(utcDate, 15, 0, tzNewYork)
        val tokyoMillis = ReminderTimeHelper.combineDateAndTime(utcDate, 15, 0, tzTokyo)

        // 15:00 in New York should be 14 hours behind 15:00 in Tokyo
        val diffHours = (nyMillis - tokyoMillis) / (3600 * 1000L)
        assertEquals(14L, diffHours)
    }

    @Test
    fun testZeroOrNegativeTimestampRejected() {
        val validationZero = ReminderTimeHelper.validateReminderTime(0L)
        assertTrue(validationZero is ReminderTimeHelper.ValidationResult.Invalid)

        val validationNeg = ReminderTimeHelper.validateReminderTime(-1000L)
        assertTrue(validationNeg is ReminderTimeHelper.ValidationResult.Invalid)
    }
}
