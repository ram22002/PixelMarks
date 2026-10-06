package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelmarks.core.database.model.MetadataStatus

/**
 * Database entity representing a bookmark.
 * Foreign key to Collection uses SET_NULL so deleting a collection never deletes bookmarks.
 * normalizedUrl is deliberately non-unique at schema level to allow the user duplicate choice ("Save Anyway").
 */
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["normalizedUrl"]),
        Index(value = ["collectionId"]),
        Index(value = ["domain"]),
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isPinned"]),
        Index(value = ["isArchived"]),
        Index(value = ["isTrashed"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val normalizedUrl: String,
    val title: String,
    val description: String? = null,
    val domain: String,
    val faviconUrl: String? = null,
    val thumbnailUrl: String? = null,
    val collectionId: Long? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastOpenedAt: Long? = null,
    val visitCount: Int = 0,
    val reminderAt: Long? = null,
    val metadataStatus: MetadataStatus = MetadataStatus.PENDING,
    val metadataUpdatedAt: Long? = null
)
