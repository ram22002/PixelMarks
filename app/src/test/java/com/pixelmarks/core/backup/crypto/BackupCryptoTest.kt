package com.pixelmarks.core.backup.crypto

import com.pixelmarks.core.backup.model.BackupException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun `encrypt and decrypt round trip succeeds with correct password`() {
        val password = "SuperSecretPassword123!".toCharArray()
        val plaintext = "Hello PixelMarks Encrypted Backup Payload".toByteArray(Charsets.UTF_8)
        val salt = BackupCrypto.generateSalt()
        val nonce = BackupCrypto.generateNonce()
        val aad = "header-metadata-auth-data".toByteArray(Charsets.UTF_8)

        val key = BackupCrypto.deriveKey(password, salt)
        val ciphertext = BackupCrypto.encrypt(plaintext, key, nonce, aad)

        assertNotNull(ciphertext)
        assertFalse(plaintext.contentEquals(ciphertext))

        val decrypted = BackupCrypto.decrypt(ciphertext, key, nonce, aad)
        assertArrayEquals(plaintext, decrypted)
        assertEquals("Hello PixelMarks Encrypted Backup Payload", String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `decrypt fails when password is incorrect`() {
        val correctPassword = "CorrectPassword123!".toCharArray()
        val wrongPassword = "WrongPassword999!".toCharArray()
        val plaintext = "Sensitive user bookmark data".toByteArray(Charsets.UTF_8)
        val salt = BackupCrypto.generateSalt()
        val nonce = BackupCrypto.generateNonce()

        val keyCorrect = BackupCrypto.deriveKey(correctPassword, salt)
        val ciphertext = BackupCrypto.encrypt(plaintext, keyCorrect, nonce)

        val keyWrong = BackupCrypto.deriveKey(wrongPassword, salt)
        try {
            BackupCrypto.decrypt(ciphertext, keyWrong, nonce)
            fail("Expected BackupException.InvalidPasswordOrCorrupt")
        } catch (e: BackupException.InvalidPasswordOrCorrupt) {
            assertTrue(e.message!!.contains("Incorrect password"))
        }
    }

    @Test
    fun `decrypt fails when ciphertext is tampered or corrupted`() {
        val password = "StrongPassword2026!".toCharArray()
        val plaintext = "Integrity test payload".toByteArray(Charsets.UTF_8)
        val salt = BackupCrypto.generateSalt()
        val nonce = BackupCrypto.generateNonce()

        val key = BackupCrypto.deriveKey(password, salt)
        val ciphertext = BackupCrypto.encrypt(plaintext, key, nonce)

        // Corrupt one byte in ciphertext
        val tampered = ciphertext.clone()
        tampered[tampered.size / 2] = (tampered[tampered.size / 2] + 1).toByte()

        try {
            BackupCrypto.decrypt(tampered, key, nonce)
            fail("Expected authentication failure on tampered ciphertext")
        } catch (_: BackupException.InvalidPasswordOrCorrupt) {
            // Success
        }
    }

    @Test
    fun `decrypt fails when aad is modified`() {
        val password = "StrongPassword2026!".toCharArray()
        val plaintext = "Authenticated data test".toByteArray(Charsets.UTF_8)
        val salt = BackupCrypto.generateSalt()
        val nonce = BackupCrypto.generateNonce()
        val originalAad = "version=1;time=12345678".toByteArray(Charsets.UTF_8)
        val tamperedAad = "version=2;time=12345678".toByteArray(Charsets.UTF_8)

        val key = BackupCrypto.deriveKey(password, salt)
        val ciphertext = BackupCrypto.encrypt(plaintext, key, nonce, originalAad)

        try {
            BackupCrypto.decrypt(ciphertext, key, nonce, tamperedAad)
            fail("Expected authentication failure when AAD is tampered")
        } catch (_: BackupException.InvalidPasswordOrCorrupt) {
            // Success
        }
    }

    @Test
    fun `generateSalt produces unique 16 byte random values`() {
        val salt1 = BackupCrypto.generateSalt()
        val salt2 = BackupCrypto.generateSalt()

        assertEquals(16, salt1.size)
        assertEquals(16, salt2.size)
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `generateNonce produces unique 12 byte random values`() {
        val nonce1 = BackupCrypto.generateNonce()
        val nonce2 = BackupCrypto.generateNonce()

        assertEquals(12, nonce1.size)
        assertEquals(12, nonce2.size)
        assertFalse(nonce1.contentEquals(nonce2))
    }
}
