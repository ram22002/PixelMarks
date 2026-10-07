package com.pixelmarks.core.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PasswordValidationResult {
    data object Valid : PasswordValidationResult
    data class Invalid(val message: String) : PasswordValidationResult
}

object CustomPasswordManager {
    const val KDF_VERSION_1 = 1
    const val KDF_VERSION_2 = 2
    const val CURRENT_KDF_VERSION = KDF_VERSION_2

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val ITERATIONS_V1 = 100_000
    const val ITERATIONS_V2 = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 32
    const val MIN_PASSWORD_LENGTH = 8

    private val secureRandom = SecureRandom()

    fun validatePassword(password: String): PasswordValidationResult {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return PasswordValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }
        return PasswordValidationResult.Valid
    }

    fun validatePassword(password: CharArray): PasswordValidationResult {
        if (password.size < MIN_PASSWORD_LENGTH) {
            return PasswordValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }
        return PasswordValidationResult.Valid
    }

    fun isHintAcceptable(password: CharArray, hint: String?): Boolean {
        if (hint.isNullOrBlank()) return true
        val trimmedHint = hint.trim()
        val pwdString = String(password).trim()
        return !trimmedHint.equals(pwdString, ignoreCase = true)
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    suspend fun deriveKeyHex(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS_V2): String =
        withContext(Dispatchers.Default) {
            val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            val hash = skf.generateSecret(spec).encoded
            spec.clearPassword()
            val hex = bytesToHex(hash)
            hash.fill(0)
            hex
        }

    suspend fun verifyPassword(
        password: CharArray,
        saltHex: String,
        expectedVerifierHex: String,
        kdfVersion: Int = KDF_VERSION_2
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            val salt = hexToBytes(saltHex)
            val iterations = if (kdfVersion >= KDF_VERSION_2) ITERATIONS_V2 else ITERATIONS_V1
            val computedHex = deriveKeyHex(password, salt, iterations)
            val expectedBytes = hexToBytes(expectedVerifierHex)
            val computedBytes = hexToBytes(computedHex)
            val match = MessageDigest.isEqual(expectedBytes, computedBytes)
            salt.fill(0)
            expectedBytes.fill(0)
            computedBytes.fill(0)
            match
        } catch (_: Exception) {
            false
        }
    }

    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
