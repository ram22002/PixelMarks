package com.pixelmarks.core.model

import com.pixelmarks.core.database.model.MetadataStatus

/**
 * Core immutable domain model representing a Bookmark in PixelMarks.
 */
data class Bookmark(
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null,
    val visitCount: Int = 0,
    val reminderAt: Long? = null,
    val metadataStatus: MetadataStatus = MetadataStatus.PENDING,
    val metadataUpdatedAt: Long? = null
)
