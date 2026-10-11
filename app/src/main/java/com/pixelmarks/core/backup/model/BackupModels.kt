package com.pixelmarks.core.backup.model

const val CURRENT_BACKUP_FORMAT_VERSION = 1
val BACKUP_MAGIC_BYTES = byteArrayOf('P'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())

/**
 * Top-level logical payload containing all application data in a versioned format,
 * completely decoupled from the Room database file and schema.
 */
data class BackupPayload(
    val version: Int = CURRENT_BACKUP_FORMAT_VERSION,
    val appVersion: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val bookmarks: List<BackupBookmarkDto> = emptyList(),
    val collections: List<BackupCollectionDto> = emptyList(),
    val tags: List<BackupTagDto> = emptyList(),
    val bookmarkTags: List<BackupBookmarkTagRefDto> = emptyList(),
    val reminders: List<BackupReminderDto> = emptyList(),
    val preferences: BackupPreferencesDto = BackupPreferencesDto()
)

data class BackupBookmarkDto(
    val id: Long,
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
    val isPrivate: Boolean = false,
    val metadataStatus: String = "PENDING",
    val metadataUpdatedAt: Long? = null
)

data class BackupCollectionDto(
    val id: Long,
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

data class BackupTagDto(
    val id: Long,
    val name: String,
    val color: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Long
)

data class BackupBookmarkTagRefDto(
    val bookmarkId: Long,
    val tagId: Long
)

data class BackupReminderDto(
    val id: Long,
    val bookmarkId: Long,
    val reminderAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isCompleted: Boolean = false
)

data class BackupPreferencesDto(
    val browserPreference: String? = null
)

/**
 * Header metadata parsed from the unencrypted container envelope.
 */
data class BackupHeaderInfo(
    val formatVersion: Int,
    val createdAt: Long,
    val kdfIterations: Int,
    val salt: ByteArray,
    val nonce: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BackupHeaderInfo
        if (formatVersion != other.formatVersion) return false
        if (createdAt != other.createdAt) return false
        if (kdfIterations != other.kdfIterations) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = formatVersion
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + kdfIterations
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        return result
    }
}

enum class RestoreMode {
    REPLACE,
    MERGE,
    IMPORT_AS_NEW
}

data class RestoreResult(
    val bookmarksRestored: Int,
    val collectionsRestored: Int,
    val tagsRestored: Int,
    val remindersRestored: Int,
    val mode: RestoreMode
)

data class BackupExportResult(
    val fileSizeBytes: Long,
    val createdAt: Long,
    val checksum: String,
    val bookmarkCount: Int,
    val collectionCount: Int,
    val tagCount: Int
)

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidFormat(message: String = "Not a valid PixelMarks backup file") : BackupException(message)
    class UnsupportedVersion(val version: Int) : BackupException("Unsupported backup version: $version. Please update PixelMarks.")
    class InvalidPasswordOrCorrupt(message: String = "Incorrect password or corrupted backup file") : BackupException(message)
    class CorruptedFile(message: String = "Backup file is damaged or incomplete") : BackupException(message)
    class ValidationError(message: String) : BackupException(message)
    class RestoreFailed(message: String, cause: Throwable? = null) : BackupException(message, cause)
}
