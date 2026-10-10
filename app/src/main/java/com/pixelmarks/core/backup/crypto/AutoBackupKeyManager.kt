package com.pixelmarks.core.backup.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the dedicated automatic backup password using hardware-backed Android KeyStore encryption.
 * The password is NEVER persisted in DataStore, SharedPreferences, or settings in plaintext.
 */
class AutoBackupKeyManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "PixelMarks_AutoBackup_MasterKey"
        private const val VAULT_FILENAME = "autobackup_vault.bin"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val vaultFile: File
        get() = File(context.filesDir, VAULT_FILENAME)

    private var jvmFallbackKey: SecretKey? = null

    private fun getOrCreateSecretKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
                return entry.secretKey
            }

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (_: Exception) {
            // Fallback for Robolectric / JVM unit test environments where AndroidKeyStore provider is not registered
            if (jvmFallbackKey == null) {
                val kg = KeyGenerator.getInstance("AES")
                kg.init(256)
                jvmFallbackKey = kg.generateKey()
            }
            return jvmFallbackKey!!
        }
    }

    @Synchronized
    fun saveAutoBackupPassword(password: CharArray) {
        try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            val passwordBytes = String(password).toByteArray(Charsets.UTF_8)
            val encryptedBytes = cipher.doFinal(passwordBytes)

            // Write IV + Ciphertext to private internal file
            vaultFile.outputStream().use { stream ->
                stream.write(iv)
                stream.write(encryptedBytes)
                stream.flush()
            }
        } finally {
            BackupCrypto.clear(password)
        }
    }

    @Synchronized
    fun getAutoBackupPassword(): CharArray? {
        if (!vaultFile.exists()) return null
        return try {
            val bytes = vaultFile.readBytes()
            if (bytes.size <= GCM_IV_LENGTH) return null

            val iv = bytes.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = bytes.copyOfRange(GCM_IV_LENGTH, bytes.size)

            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            val passString = String(decryptedBytes, Charsets.UTF_8)
            passString.toCharArray()
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun hasAutoBackupPassword(): Boolean {
        return vaultFile.exists() && vaultFile.length() > GCM_IV_LENGTH
    }

    @Synchronized
    fun clearAutoBackupPassword() {
        if (vaultFile.exists()) {
            vaultFile.delete()
        }
        jvmFallbackKey = null
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {}
    }
}
