package com.pixelmarks.core.security.model

/**
 * Domain enum defining when PixelMarks locks automatically after moving to the background.
 */
enum class AppLockTimeout(val durationMillis: Long, val label: String) {
    IMMEDIATELY(0L, "Immediately"),
    ONE_MINUTE(60_000L, "After 1 minute"),
    FIVE_MINUTES(300_000L, "After 5 minutes"),
    FIFTEEN_MINUTES(900_000L, "After 15 minutes");

    companion object {
        fun fromString(value: String?): AppLockTimeout {
            if (value.isNullOrBlank()) return IMMEDIATELY
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: IMMEDIATELY
        }
    }
}
