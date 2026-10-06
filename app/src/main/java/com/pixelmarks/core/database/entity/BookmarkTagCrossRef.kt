package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cross-reference entity modeling the many-to-many relationship between bookmarks and tags.
 * Composite primary key prevents duplicate tag assignments.
 * Cascading deletes ensure orphan relationship records are pruned when either side is deleted.
 */
@Entity(
    tableName = "bookmark_tag_cross_ref",
    primaryKeys = ["bookmarkId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = BookmarkEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tagId"]),
        Index(value = ["bookmarkId"])
    ]
)
data class BookmarkTagCrossRef(
    val bookmarkId: Long,
    val tagId: Long
)
