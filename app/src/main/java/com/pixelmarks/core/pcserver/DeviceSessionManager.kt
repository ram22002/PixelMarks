package com.pixelmarks.core.pcserver

import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.database.model.DeviceType
import com.pixelmarks.data.repository.ConnectedDeviceRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Manages device session tokens using cryptographic hashing.
 * Plaintext tokens are NEVER stored in persistent storage or logged.
 */
class DeviceSessionManager(
    private val repository: ConnectedDeviceRepository
) {

    private val secureRandom = SecureRandom()

    /**
     * Generates a 256-bit cryptographically secure token, hashes it with SHA-256,
     * stores the hash in the database, and returns the plaintext token for one-time issuance.
     */
    suspend fun createDeviceSession(
        deviceName: String,
        deviceType: DeviceType = DeviceType.BROWSER
    ): Pair<Long, String> {
        val rawBytes = ByteArray(32)
        secureRandom.nextBytes(rawBytes)
        val plaintextToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes)
        val tokenHash = hashToken(plaintextToken)

        val deviceId = repository.registerDevice(
            deviceName = deviceName.ifBlank { "PC Browser" },
            tokenHash = tokenHash,
            deviceType = deviceType
        )

        return Pair(deviceId, plaintextToken)
    }

    /**
     * Authenticates an incoming Bearer token.
     * Returns the authorized ConnectedDeviceEntity, or null if invalid/revoked/blocked.
     */
    suspend fun authenticateToken(token: String?): ConnectedDeviceEntity? {
        if (token.isNullOrBlank()) return null
        val tokenHash = hashToken(token.trim())
        val device = repository.getDeviceByTokenHash(tokenHash) ?: return null

        if (device.revokedAt != null || device.isBlocked) {
            return null
        }

        // Asynchronously update last seen
        repository.updateLastSeen(device.id)
        return device
    }

    suspend fun revokeDevice(deviceId: Long) {
        repository.revokeDevice(deviceId)
    }

    suspend fun revokeAllDevices() {
        repository.revokeAllDevices()
    }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
