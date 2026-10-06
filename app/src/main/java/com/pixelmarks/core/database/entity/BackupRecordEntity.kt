package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType

/**
 * Database entity tracking local and remote backup archive records.
 */
@Entity(
    tableName = "backup_records",
    indices = [
        Index(value = ["createdAt"])
    ]
)
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: BackupType,
    val location: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val checksum: String,
    val status: BackupStatus
)
