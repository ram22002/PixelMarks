package com.pixelmarks.core.database.model

/**
 * Representation of website metadata scraping status.
 */
enum class MetadataStatus {
    PENDING,
    FETCHING,
    SUCCESS,
    FAILED,
    BLOCKED,
    NO_METADATA
}

/**
 * Device types for connected PC client pairing.
 */
enum class DeviceType {
    BROWSER,
    DESKTOP_APP,
    OTHER
}

/**
 * Backup destination and export types.
 */
enum class BackupType {
    LOCAL,
    GOOGLE_DRIVE,
    PC_EXPORT
}

/**
 * Status of backup operations.
 */
enum class BackupStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS
}
