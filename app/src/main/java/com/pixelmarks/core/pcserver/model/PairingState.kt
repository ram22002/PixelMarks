package com.pixelmarks.core.pcserver.model

/**
 * State representing the pairing lifecycle between phone and PC browser.
 */
sealed interface PairingState {
    object Idle : PairingState

    data class Active(
        val code: String,
        val formattedCode: String,
        val expiresAt: Long,
        val qrContent: String
    ) : PairingState

    data class PendingConfirmation(
        val requestId: String,
        val code: String,
        val deviceName: String,
        val clientIp: String,
        val userAgent: String,
        val requestedAt: Long
    ) : PairingState

    data class Locked(
        val lockedUntil: Long,
        val message: String
    ) : PairingState
}

/**
 * Result of user confirmation on phone.
 */
sealed interface PairingDecision {
    data class Approved(val sessionToken: String) : PairingDecision
    object Denied : PairingDecision
    object TimedOut : PairingDecision
}
