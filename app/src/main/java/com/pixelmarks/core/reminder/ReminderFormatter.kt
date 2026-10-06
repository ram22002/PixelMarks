package com.pixelmarks.core.reminder

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formatter for human-readable reminder timestamps.
 * Examples:
 * - "Today · 8:00 PM"
 * - "Tomorrow · 9:30 AM"
 * - "Oct 12 · 7:00 PM"
 */
object ReminderFormatter {

    fun format(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(timestamp))

        val isSameYear = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val dayOfYearDiff = targetCal.get(Calendar.DAY_OF_YEAR) - nowCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayOfYearDiff == 0 -> "Today · $formattedTime"
            isSameYear && dayOfYearDiff == 1 -> "Tomorrow · $formattedTime"
            isSameYear -> {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                "${dateFormat.format(Date(timestamp))} · $formattedTime"
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                "${dateFormat.format(Date(timestamp))} · $formattedTime"
            }
        }
    }
}
