package com.pixelmarks.core.database

import androidx.room.TypeConverter
import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType
import com.pixelmarks.core.database.model.DeviceType
import com.pixelmarks.core.database.model.MetadataStatus

/**
 * Room TypeConverters for stable database enum persistence.
 * Avoids storing ordinal numbers to prevent data corruption during enum refactoring.
 */
class DatabaseConverters {

    @TypeConverter
    fun fromMetadataStatus(status: MetadataStatus?): String? = status?.name

    @TypeConverter
    fun toMetadataStatus(value: String?): MetadataStatus? =
        value?.let { runCatching { MetadataStatus.valueOf(it) }.getOrDefault(MetadataStatus.PENDING) }

    @TypeConverter
    fun fromDeviceType(type: DeviceType?): String? = type?.name

    @TypeConverter
    fun toDeviceType(value: String?): DeviceType? =
        value?.let { runCatching { DeviceType.valueOf(it) }.getOrDefault(DeviceType.OTHER) }

    @TypeConverter
    fun fromBackupType(type: BackupType?): String? = type?.name

    @TypeConverter
    fun toBackupType(value: String?): BackupType? =
        value?.let { runCatching { BackupType.valueOf(it) }.getOrDefault(BackupType.LOCAL) }

    @TypeConverter
    fun fromBackupStatus(status: BackupStatus?): String? = status?.name

    @TypeConverter
    fun toBackupStatus(value: String?): BackupStatus? =
        value?.let { runCatching { BackupStatus.valueOf(it) }.getOrDefault(BackupStatus.FAILED) }
}
