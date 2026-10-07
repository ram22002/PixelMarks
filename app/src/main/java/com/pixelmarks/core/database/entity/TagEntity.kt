package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity representing user tags.
 * Unique index on name prevents duplicate tag creation.
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name", "isPrivate"], unique = true),
        Index(value = ["isPrivate"])
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Long
)
