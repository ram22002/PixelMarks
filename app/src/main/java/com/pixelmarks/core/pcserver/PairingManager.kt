package com.pixelmarks.core.pcserver

import com.pixelmarks.core.pcserver.model.PairingDecision
import com.pixelmarks.core.pcserver.model.PairingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom
import java.util.UUID

/**
 * Manages short-lived pairing credentials, brute-force mitigation, and on-device confirmation.
 */
class PairingManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val codeLifetimeMillis: Long = DEFAULT_CODE_LIFETIME_MS,
    private val maxFailedAttempts: Int = DEFAULT_MAX_FAILED_ATTEMPTS,
    private val lockoutDurationMillis: Long = DEFAULT_LOCKOUT_MS
) {

    private val secureRandom = SecureRandom()
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private var activeCode: String? = null
    private var codeExpiresAt: Long = 0L
    private var isCodeConsumed: Boolean = false
    private var failedAttempts: Int = 0
    private var lockedUntil: Long = 0L

    // Map of active pending approval requests (requestId -> Deferred<PairingDecision>)
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<PairingDecision>>()

    sealed interface InitiateResult {
        data class AwaitingApproval(val requestId: String) : InitiateResult
        data class Error(val message: String, val isLocked: Boolean = false) : InitiateResult
    }

    /**
     * Generates a new short-lived, single-use 6-digit numeric pairing code.
     */
    @Synchronized
    fun generateNewCode(serverBaseUrl: String): PairingState.Active {
        checkLockout()
        val now = System.currentTimeMillis()
        if (now < lockedUntil) {
            val lockedState = PairingState.Locked(
                lockedUntil = lockedUntil,
                message = "Too many failed attempts. Try again in ${((lockedUntil - now) / 1000)} seconds."
            )
            _pairingState.value = lockedState
            throw IllegalStateException(lockedState.message)
        }

        // Generate 6 digit code (100000..999999)
        val num = 100_000 + secureRandom.nextInt(900_000)
        val code = num.toString()
        val formatted = "${code.substring(0, 3)}-${code.substring(3)}"
        val expiresAt = now + codeLifetimeMillis

        activeCode = code
        codeExpiresAt = expiresAt
        isCodeConsumed = false

        // QR contains connection URL with pairing code parameter for one-scan entry
        val qrUrl = if (serverBaseUrl.endsWith("/")) {
            "${serverBaseUrl}index.html?pairCode=$code"
        } else {
            "$serverBaseUrl/index.html?pairCode=$code"
        }

        val state = PairingState.Active(
            code = code,
            formattedCode = formatted,
            expiresAt = expiresAt,
            qrContent = qrUrl
        )
        _pairingState.value = state
        return state
    }

    /**
     * Called when a PC browser attempts to initiate pairing with a code.
     */
    @Synchronized
    fun initiatePairing(
        inputCode: String,
        deviceName: String,
        clientIp: String,
        userAgent: String
    ): InitiateResult {
        val now = System.currentTimeMillis()
        if (now < lockedUntil) {
            val remainingSec = (lockedUntil - now) / 1000
            return InitiateResult.Error("Too many failed attempts. Pairing locked for $remainingSec seconds.", isLocked = true)
        }

        val sanitizedInput = inputCode.replace("-", "").trim()
        val currentCode = activeCode

        if (currentCode == null || isCodeConsumed || now > codeExpiresAt) {
            recordFailedAttempt()
            return InitiateResult.Error("Pairing code has expired or is invalid. Please generate a new code.")
        }

        if (sanitizedInput != currentCode) {
            recordFailedAttempt()
            return InitiateResult.Error("Invalid pairing code.")
        }

        // Code matches and is valid! Set up on-device approval request
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<PairingDecision>()
        pendingRequests[requestId] = deferred

        _pairingState.value = PairingState.PendingConfirmation(
            requestId = requestId,
            code = currentCode,
            deviceName = deviceName.ifBlank { "PC Browser" },
            clientIp = clientIp,
            userAgent = userAgent,
            requestedAt = now
        )

        return InitiateResult.AwaitingApproval(requestId)
    }

    /**
     * Awaits decision on a pending pairing request (suspends up to timeoutMillis).
     */
    suspend fun awaitDecision(requestId: String, timeoutMillis: Long = 60_000L): PairingDecision {
        val deferred = synchronized(this) { pendingRequests[requestId] }
            ?: return PairingDecision.TimedOut

        val decision = withTimeoutOrNull(timeoutMillis) {
            deferred.await()
        } ?: PairingDecision.TimedOut

        synchronized(this) {
            pendingRequests.remove(requestId)
            if (decision is PairingDecision.Approved) {
                // Consume code for replay protection
                isCodeConsumed = true
                activeCode = null
                failedAttempts = 0
                _pairingState.value = PairingState.Idle
            } else if (decision is PairingDecision.Denied) {
                recordFailedAttempt()
                // Revert state back to Active code or Idle
                val current = activeCode
                if (current != null && !isCodeConsumed && System.currentTimeMillis() < codeExpiresAt) {
                    val formatted = "${current.substring(0, 3)}-${current.substring(3)}"
                    _pairingState.value = PairingState.Active(
                        code = current,
                        formattedCode = formatted,
                        expiresAt = codeExpiresAt,
                        qrContent = ""
                    )
                } else {
                    _pairingState.value = PairingState.Idle
                }
            } else if (decision is PairingDecision.TimedOut) {
                _pairingState.value = PairingState.Idle
            }
        }

        return decision
    }

    /**
     * Called when the user on the Android device confirms pairing.
     */
    @Synchronized
    fun approvePairing(requestId: String, sessionToken: String): Boolean {
        val deferred = pendingRequests[requestId] ?: return false
        _pairingState.value = PairingState.Idle
        val completed = deferred.complete(PairingDecision.Approved(sessionToken))
        return completed
    }

    /**
     * Called when the user on the Android device rejects pairing.
     */
    @Synchronized
    fun denyPairing(requestId: String): Boolean {
        val deferred = pendingRequests[requestId] ?: return false
        _pairingState.value = PairingState.Idle
        val completed = deferred.complete(PairingDecision.Denied)
        return completed
    }


    @Synchronized
    fun cancelActiveCode() {
        activeCode = null
        isCodeConsumed = true
        pendingRequests.values.forEach { it.complete(PairingDecision.Denied) }
        pendingRequests.clear()
        _pairingState.value = PairingState.Idle
    }

    @Synchronized
    private fun recordFailedAttempt() {
        failedAttempts++
        if (failedAttempts >= maxFailedAttempts) {
            lockedUntil = System.currentTimeMillis() + lockoutDurationMillis
            _pairingState.value = PairingState.Locked(
                lockedUntil = lockedUntil,
                message = "Too many failed attempts. Pairing locked for ${lockoutDurationMillis / 1000} seconds."
            )
        }
    }

    @Synchronized
    private fun checkLockout() {
        if (lockedUntil != 0L && System.currentTimeMillis() >= lockedUntil) {
            lockedUntil = 0L
            failedAttempts = 0
            if (_pairingState.value is PairingState.Locked) {
                _pairingState.value = PairingState.Idle
            }
        }
    }

    companion object {
        const val DEFAULT_CODE_LIFETIME_MS = 5 * 60 * 1000L // 5 minutes
        const val DEFAULT_MAX_FAILED_ATTEMPTS = 5
        const val DEFAULT_LOCKOUT_MS = 5 * 60 * 1000L // 5 minutes lockout
    }
}
