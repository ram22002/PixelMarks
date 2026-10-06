package com.pixelmarks.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelmarks.core.database.model.DeviceType

/**
 * Database entity representing a paired desktop PC browser or client.
 * Stores only cryptographic token hashes (never plaintext tokens).
 */
@Entity(
    tableName = "connected_devices",
    indices = [
        Index(value = ["tokenHash"], unique = true)
    ]
)
data class ConnectedDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val deviceType: DeviceType = DeviceType.BROWSER,
    val tokenHash: String,
    val createdAt: Long,
    val lastSeenAt: Long,
    val revokedAt: Long? = null,
    val isBlocked: Boolean = false
)
