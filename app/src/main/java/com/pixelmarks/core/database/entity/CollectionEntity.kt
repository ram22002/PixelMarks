package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity representing hierarchical collections/folders.
 * Self-referencing parentId supports arbitrary nesting depth.
 */
@Entity(
    tableName = "collections",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["name"]),
        Index(value = ["domain"]),
        Index(value = ["isPrivate"])
    ]
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val icon: String? = null,
    val color: String? = null,
    val position: Int = 0,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val isPrivate: Boolean = false,
    val isDomain: Boolean = false,
    val domain: String? = null,
    val description: String? = null,
    val keywords: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
