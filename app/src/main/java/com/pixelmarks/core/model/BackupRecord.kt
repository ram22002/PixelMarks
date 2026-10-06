package com.pixelmarks.core.model

import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType

/**
 * Domain model representing an encrypted backup snapshot record.
 */
data class BackupRecord(
    val id: Long = 0,
    val type: BackupType,
    val location: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long,
    val checksum: String,
    val status: BackupStatus
)
