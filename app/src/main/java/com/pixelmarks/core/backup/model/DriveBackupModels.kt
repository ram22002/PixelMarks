package com.pixelmarks.core.backup.model

/**
 * Representation of a PixelMarks backup file stored in Google Drive.
 */
data class DriveBackupFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val modifiedAt: Long
)

/**
 * Connected Google Account information.
 */
data class GoogleAccountInfo(
    val email: String,
    val displayName: String? = null
)

/**
 * State of Google Drive connection.
 */
enum class DriveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTH_EXPIRED
}

/**
 * Schedule frequency for automatic cloud backups.
 */
enum class AutoBackupFrequency(val displayName: String, val intervalDays: Long) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7);

    companion object {
        fun fromString(value: String?): AutoBackupFrequency {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}

/**
 * Preferences governing automatic cloud backups.
 * Note: Never store backup passwords or encryption keys in these preferences.
 */
data class AutoBackupPreferences(
    val enabled: Boolean = false,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.DAILY,
    val wifiOnly: Boolean = true,
    val lastAutoBackupTime: Long? = null
)

sealed class DriveException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotConnected(message: String = "Google Drive is not connected") : DriveException(message)
    class AuthExpired(message: String = "Google authorization expired or was revoked. Please reconnect.") : DriveException(message)
    class NetworkError(message: String = "Network error connecting to Google Drive", cause: Throwable? = null) : DriveException(message, cause)
    class FileNotFound(message: String = "Backup file not found in Google Drive") : DriveException(message)
    class UploadFailed(message: String = "Failed to upload backup to Google Drive", cause: Throwable? = null) : DriveException(message, cause)
    class DownloadFailed(message: String = "Failed to download backup from Google Drive", cause: Throwable? = null) : DriveException(message, cause)
}
