package com.pixelmarks.core.security

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomPasswordManagerTest {

    @Test
    fun `validatePassword accepts 8 or more characters`() {
        val result = CustomPasswordManager.validatePassword("Secret123!")
        assertTrue(result is PasswordValidationResult.Valid)
    }

    @Test
    fun `validatePassword rejects fewer than 8 characters`() {
        val result = CustomPasswordManager.validatePassword("1234567")
        assertTrue(result is PasswordValidationResult.Invalid)
        assertEquals("Password must be at least 8 characters.", (result as PasswordValidationResult.Invalid).message)
    }

    @Test
    fun `isHintAcceptable rejects hint identical to password`() {
        val pwd = "SuperSecretPassword123".toCharArray()
        assertFalse(CustomPasswordManager.isHintAcceptable(pwd, "SuperSecretPassword123"))
        assertFalse(CustomPasswordManager.isHintAcceptable(pwd, "supersecretpassword123"))
        assertTrue(CustomPasswordManager.isHintAcceptable(pwd, "My birthday in Paris"))
        assertTrue(CustomPasswordManager.isHintAcceptable(pwd, null))
        assertTrue(CustomPasswordManager.isHintAcceptable(pwd, ""))
    }

    @Test
    fun `generateSalt produces unique random 32-byte salts`() {
        val salt1 = CustomPasswordManager.generateSalt()
        val salt2 = CustomPasswordManager.generateSalt()
        assertEquals(32, salt1.size)
        assertEquals(32, salt2.size)
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `deriveKeyHex produces consistent deterministic hash for same salt and password`() = runBlocking {
        val salt = CustomPasswordManager.generateSalt()
        val hash1 = CustomPasswordManager.deriveKeyHex("MyP@ssw0rd".toCharArray(), salt)
        val hash2 = CustomPasswordManager.deriveKeyHex("MyP@ssw0rd".toCharArray(), salt)
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // 256 bits = 32 bytes = 64 hex chars
    }

    @Test
    fun `deriveKeyHex produces different hash for different password`() = runBlocking {
        val salt = CustomPasswordManager.generateSalt()
        val hash1 = CustomPasswordManager.deriveKeyHex("Password123".toCharArray(), salt)
        val hash2 = CustomPasswordManager.deriveKeyHex("Password124".toCharArray(), salt)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verifyPassword returns true for correct password with v2 KDF`() = runBlocking {
        val salt = CustomPasswordManager.generateSalt()
        val saltHex = CustomPasswordManager.bytesToHex(salt)
        val verifierHex = CustomPasswordManager.deriveKeyHex("CorrectHorseBatteryStaple".toCharArray(), salt, CustomPasswordManager.ITERATIONS_V2)

        val isMatch = CustomPasswordManager.verifyPassword(
            "CorrectHorseBatteryStaple".toCharArray(),
            saltHex,
            verifierHex,
            kdfVersion = CustomPasswordManager.KDF_VERSION_2
        )
        assertTrue(isMatch)
    }

    @Test
    fun `verifyPassword backward compatible with v1 KDF (100k iterations)`() = runBlocking {
        val salt = CustomPasswordManager.generateSalt()
        val saltHex = CustomPasswordManager.bytesToHex(salt)
        val verifierHex = CustomPasswordManager.deriveKeyHex("LegacyPassword123".toCharArray(), salt, CustomPasswordManager.ITERATIONS_V1)

        val isMatch = CustomPasswordManager.verifyPassword(
            "LegacyPassword123".toCharArray(),
            saltHex,
            verifierHex,
            kdfVersion = CustomPasswordManager.KDF_VERSION_1
        )
        assertTrue(isMatch)
    }

    @Test
    fun `verifyPassword returns false for incorrect password`() = runBlocking {
        val salt = CustomPasswordManager.generateSalt()
        val saltHex = CustomPasswordManager.bytesToHex(salt)
        val verifierHex = CustomPasswordManager.deriveKeyHex("CorrectPassword".toCharArray(), salt)

        val isMatch = CustomPasswordManager.verifyPassword(
            "WrongPassword".toCharArray(),
            saltHex,
            verifierHex
        )
        assertFalse(isMatch)
    }

    @Test
    fun `hexToBytes and bytesToHex are reversible`() {
        val original = byteArrayOf(0x00, 0x1A, 0x2B, 0xFF.toByte(), 0x7E)
        val hex = CustomPasswordManager.bytesToHex(original)
        val decoded = CustomPasswordManager.hexToBytes(hex)
        assertTrue(original.contentEquals(decoded))
    }
}
