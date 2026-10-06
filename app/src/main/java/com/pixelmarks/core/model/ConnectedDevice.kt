package com.pixelmarks.core.model

import com.pixelmarks.core.database.model.DeviceType

/**
 * Domain model representing a paired PC client device.
 */
data class ConnectedDevice(
    val id: Long = 0,
    val deviceName: String,
    val deviceType: DeviceType = DeviceType.BROWSER,
    val tokenHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val revokedAt: Long? = null,
    val isBlocked: Boolean = false
)
