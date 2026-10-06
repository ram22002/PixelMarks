package com.pixelmarks.core.reminder

import java.util.Calendar
import java.util.TimeZone

/**
 * Utility helper for calculating, validating, and converting custom reminder dates and times
 * between UTC date picker components and local device timezone timestamps.
 */
object ReminderTimeHelper {

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val message: String) : ValidationResult
    }

    /**
     * Converts a UTC midnight millisecond timestamp (from DatePicker) and local hour/minute
     * into a local epoch millisecond timestamp using the specified [timeZone].
     */
    fun combineDateAndTime(
        dateUtcMidnightMillis: Long,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateUtcMidnightMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val dayOfMonth = utcCal.get(Calendar.DAY_OF_MONTH)

        val localCal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    /**
     * Determines whether the given UTC date millisecond timestamp is today or in the future
     * in the specified [timeZone]. Past dates are rejected.
     */
    fun isDateSelectable(
        utcTimeMillis: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcTimeMillis
        }
        val endOfDayLocal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return endOfDayLocal.timeInMillis >= now
    }

    /**
     * Checks if the date represented by [dateUtcMidnightMillis] is the same calendar day
     * as [now] in [timeZone].
     */
    fun isDateToday(
        dateUtcMidnightMillis: Long,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateUtcMidnightMillis
        }
        val nowCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
        }
        return utcCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                utcCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
                utcCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Validates whether [targetTimestamp] is a valid future reminder time.
     */
    fun validateReminderTime(
        targetTimestamp: Long,
        now: Long = System.currentTimeMillis()
    ): ValidationResult {
        return when {
            targetTimestamp <= 0L -> ValidationResult.Invalid("Invalid reminder timestamp")
            targetTimestamp <= now -> ValidationResult.Invalid("Please select a time in the future")
            else -> ValidationResult.Valid
        }
    }
}
