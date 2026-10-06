package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity representing a scheduled read-later reminder.
 * Cascade foreign key ensures reminder is cleaned up if parent bookmark is deleted.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookmarkId"]),
        Index(value = ["reminderAt"]),
        Index(value = ["isCompleted"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookmarkId: Long,
    val reminderAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isCompleted: Boolean = false
)
