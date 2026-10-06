package com.pixelmarks.core.model

/**
 * Domain model representing a scheduled bookmark reminder.
 */
data class Reminder(
    val id: Long = 0,
    val bookmarkId: Long,
    val reminderAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
