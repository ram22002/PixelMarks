package com.pixelmarks.data.mapper

import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.Reminder
import com.pixelmarks.core.model.Tag

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    url = url,
    normalizedUrl = normalizedUrl,
    title = title,
    description = description,
    domain = domain,
    faviconUrl = faviconUrl,
    thumbnailUrl = thumbnailUrl,
    collectionId = collectionId,
    isFavorite = isFavorite,
    isPinned = isPinned,
    isArchived = isArchived,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastOpenedAt = lastOpenedAt,
    visitCount = visitCount,
    reminderAt = reminderAt,
    metadataStatus = metadataStatus,
    metadataUpdatedAt = metadataUpdatedAt
)

fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    url = url,
    normalizedUrl = normalizedUrl,
    title = title,
    description = description,
    domain = domain,
    faviconUrl = faviconUrl,
    thumbnailUrl = thumbnailUrl,
    collectionId = collectionId,
    isFavorite = isFavorite,
    isPinned = isPinned,
    isArchived = isArchived,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastOpenedAt = lastOpenedAt,
    visitCount = visitCount,
    reminderAt = reminderAt,
    metadataStatus = metadataStatus,
    metadataUpdatedAt = metadataUpdatedAt
)

fun CollectionEntity.toDomain(): Collection = Collection(
    id = id,
    name = name,
    parentId = parentId,
    icon = icon,
    color = color,
    position = position,
    isLocked = isLocked,
    isHidden = isHidden,
    isDomain = isDomain,
    domain = domain,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Collection.toEntity(): CollectionEntity = CollectionEntity(
    id = id,
    name = name,
    parentId = parentId,
    icon = icon,
    color = color,
    position = position,
    isLocked = isLocked,
    isHidden = isHidden,
    isDomain = isDomain,
    domain = domain,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    color = color,
    createdAt = createdAt
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    color = color,
    createdAt = createdAt
)

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    bookmarkId = bookmarkId,
    reminderAt = reminderAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isCompleted = isCompleted
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    bookmarkId = bookmarkId,
    reminderAt = reminderAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isCompleted = isCompleted
)
