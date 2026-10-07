package com.pixelmarks.core.backup.crypto

import com.pixelmarks.core.backup.model.BackupException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val KDF_ITERATIONS = 120_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16
    const val NONCE_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun generateNonce(): ByteArray {
        val nonce = ByteArray(NONCE_LENGTH_BYTES)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = KDF_ITERATIONS
    ): SecretKey {
        val keySpec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            val keyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = keyFactory.generateSecret(keySpec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            keySpec.clearPassword()
        }
    }

    fun encrypt(
        plaintext: ByteArray,
        key: SecretKey,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            if (aad != null && aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }
            return cipher.doFinal(plaintext)
        } catch (e: GeneralSecurityException) {
            throw BackupException.CorruptedFile("Failed to encrypt backup data: ${e.message}")
        }
    }

    fun decrypt(
        ciphertext: ByteArray,
        key: SecretKey,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            if (aad != null && aad.isNotEmpty()) {
                cipher.updateAAD(aad)
            }
            return cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw BackupException.InvalidPasswordOrCorrupt("Incorrect password or corrupted backup file")
        } catch (e: GeneralSecurityException) {
            throw BackupException.CorruptedFile("Decryption error: ${e.message}")
        }
    }

    fun clear(array: CharArray) {
        Arrays.fill(array, ' ')
    }

    fun clear(array: ByteArray) {
        Arrays.fill(array, 0.toByte())
    }
}
