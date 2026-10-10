package com.pixelmarks.core.backup.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AutoBackupKeyManagerTest {

    private lateinit var context: Context
    private lateinit var keyManager: AutoBackupKeyManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        keyManager = AutoBackupKeyManager(context)
        keyManager.clearAutoBackupPassword()
    }

    @After
    fun tearDown() {
        keyManager.clearAutoBackupPassword()
    }

    @Test
    fun saveAndRetrievePassword_success() {
        assertFalse(keyManager.hasAutoBackupPassword())

        val password = "StrongPassword789#".toCharArray()
        keyManager.saveAutoBackupPassword(password)

        assertTrue(keyManager.hasAutoBackupPassword())
        val retrieved = keyManager.getAutoBackupPassword()
        assertNotNull(retrieved)
        assertEquals("StrongPassword789#", String(retrieved!!))
    }

    @Test
    fun noPlaintextPersistence_vaultFileIsEncrypted() {
        val secretPass = "SuperSecretPlainTextValue123".toCharArray()
        keyManager.saveAutoBackupPassword(secretPass)

        val vaultFile = File(context.filesDir, "autobackup_vault.bin")
        assertTrue(vaultFile.exists())

        val rawFileContent = vaultFile.readBytes()
        val rawString = String(rawFileContent, Charsets.ISO_8859_1)

        // The plaintext password MUST NOT appear anywhere in the raw file bytes
        assertFalse("Vault file contains plaintext password!", rawString.contains("SuperSecretPlainTextValue123"))
    }

    @Test
    fun clearPassword_removesVaultFile() {
        keyManager.saveAutoBackupPassword("TempPass123!".toCharArray())
        assertTrue(keyManager.hasAutoBackupPassword())

        keyManager.clearAutoBackupPassword()
        assertFalse(keyManager.hasAutoBackupPassword())
        val retrieved = keyManager.getAutoBackupPassword()
        assertEquals(null, retrieved)
    }
}
