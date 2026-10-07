package com.pixelmarks.core.backup.validation

import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupPayload
import com.pixelmarks.core.backup.model.CURRENT_BACKUP_FORMAT_VERSION
import com.pixelmarks.core.browser.WebUrlValidator

object BackupValidator {

    /**
     * Validates logical integrity of the decrypted backup payload.
     * Throws BackupException.ValidationError if semantic corruption or invalid relationships are found.
     */
    fun validate(payload: BackupPayload) {
        if (payload.version < 1) {
            throw BackupException.ValidationError("Invalid backup payload version: ${payload.version}")
        }
        if (payload.version > CURRENT_BACKUP_FORMAT_VERSION) {
            throw BackupException.UnsupportedVersion(payload.version)
        }

        // Validate Bookmarks
        val bookmarkIds = mutableSetOf<Long>()
        payload.bookmarks.forEach { bookmark ->
            if (bookmark.url.isBlank()) {
                throw BackupException.ValidationError("Bookmark ${bookmark.id} has blank URL")
            }
            if (!WebUrlValidator.isValidWebUrl(bookmark.url)) {
                throw BackupException.ValidationError("Bookmark ${bookmark.id} has invalid URL: ${bookmark.url}")
            }
            if (bookmark.title.isBlank()) {
                throw BackupException.ValidationError("Bookmark ${bookmark.id} has blank title")
            }
            bookmarkIds.add(bookmark.id)
        }

        // Validate Collections hierarchy
        val collectionIds = mutableSetOf<Long>()
        val collectionParentMap = mutableMapOf<Long, Long?>()

        payload.collections.forEach { collection ->
            if (collection.name.isBlank()) {
                throw BackupException.ValidationError("Collection ${collection.id} has blank name")
            }
            if (collection.parentId != null && collection.parentId == collection.id) {
                throw BackupException.ValidationError("Collection ${collection.id} cannot be its own parent")
            }
            collectionIds.add(collection.id)
            collectionParentMap[collection.id] = collection.parentId
        }

        // Check for hierarchy cycles in collections
        for (colId in collectionIds) {
            val visited = mutableSetOf<Long>()
            var current: Long? = colId
            while (current != null) {
                if (visited.contains(current)) {
                    throw BackupException.ValidationError("Cyclic parent reference detected in collection $colId")
                }
                visited.add(current)
                current = collectionParentMap[current]
            }
        }

        // Validate bookmark collection references
        payload.bookmarks.forEach { bookmark ->
            if (bookmark.collectionId != null && !collectionIds.contains(bookmark.collectionId)) {
                // Warning/safe fallback: if collection doesn't exist, we will safely set collectionId to null during restore
            }
        }

        // Validate Tags
        val tagIds = mutableSetOf<Long>()
        payload.tags.forEach { tag ->
            if (tag.name.isBlank()) {
                throw BackupException.ValidationError("Tag ${tag.id} has blank name")
            }
            tagIds.add(tag.id)
        }

        // Validate BookmarkTagCrossRef
        payload.bookmarkTags.forEach { ref ->
            if (!bookmarkIds.contains(ref.bookmarkId)) {
                throw BackupException.ValidationError("BookmarkTag reference points to missing bookmark: ${ref.bookmarkId}")
            }
            if (!tagIds.contains(ref.tagId)) {
                throw BackupException.ValidationError("BookmarkTag reference points to missing tag: ${ref.tagId}")
            }
        }

        // Validate Reminders
        payload.reminders.forEach { reminder ->
            if (!bookmarkIds.contains(reminder.bookmarkId)) {
                throw BackupException.ValidationError("Reminder points to missing bookmark: ${reminder.bookmarkId}")
            }
            if (reminder.reminderAt <= 0) {
                throw BackupException.ValidationError("Reminder has invalid trigger timestamp: ${reminder.reminderAt}")
            }
        }
    }
}
