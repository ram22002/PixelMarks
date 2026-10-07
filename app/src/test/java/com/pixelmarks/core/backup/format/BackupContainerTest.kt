package com.pixelmarks.core.backup.format

import com.pixelmarks.core.backup.model.BACKUP_MAGIC_BYTES
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.CURRENT_BACKUP_FORMAT_VERSION
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupContainerTest {

    @Test
    fun `writeEnvelope and readEnvelope round trip matches exactly`() {
        val header = BackupHeaderInfo(
            formatVersion = CURRENT_BACKUP_FORMAT_VERSION,
            createdAt = 1728300000000L,
            kdfIterations = 120_000,
            salt = ByteArray(16) { it.toByte() },
            nonce = ByteArray(12) { (it * 2).toByte() }
        )
        val ciphertext = "MockEncryptedPayloadCiphertextBytes".toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        BackupContainer.writeEnvelope(baos, header, ciphertext)
        val fileBytes = baos.toByteArray()

        assertTrue(fileBytes.size > 4 + 4 + 8 + 4 + 4 + 16 + 4 + 12 + 4)

        // Read envelope
        val bais = ByteArrayInputStream(fileBytes)
        val (readHeader, readCiphertext) = BackupContainer.readEnvelope(bais)

        assertEquals(header.formatVersion, readHeader.formatVersion)
        assertEquals(header.createdAt, readHeader.createdAt)
        assertEquals(header.kdfIterations, readHeader.kdfIterations)
        assertArrayEquals(header.salt, readHeader.salt)
        assertArrayEquals(header.nonce, readHeader.nonce)
        assertArrayEquals(ciphertext, readCiphertext)
    }

    @Test
    fun `readHeader with invalid magic bytes throws InvalidFormat`() {
        val invalidBytes = byteArrayOf('B'.code.toByte(), 'A'.code.toByte(), 'D'.code.toByte(), '!'.code.toByte(), 0, 0, 0, 1)
        val bais = ByteArrayInputStream(invalidBytes)

        try {
            BackupContainer.readHeader(bais)
            fail("Expected InvalidFormat for wrong magic header")
        } catch (e: BackupException.InvalidFormat) {
            assertTrue(e.message!!.contains("magic header mismatch"))
        }
    }

    @Test
    fun `readHeader with future format version throws UnsupportedVersion`() {
        val baos = ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(baos)
        dos.write(BACKUP_MAGIC_BYTES)
        dos.writeInt(99) // Future unsupported format version
        dos.writeLong(System.currentTimeMillis())
        dos.writeInt(120_000)
        dos.writeInt(16)
        dos.write(ByteArray(16))
        dos.writeInt(12)
        dos.write(ByteArray(12))
        dos.writeInt(10)
        dos.write(ByteArray(10))

        val bais = ByteArrayInputStream(baos.toByteArray())
        try {
            BackupContainer.readHeader(bais)
            fail("Expected UnsupportedVersion exception")
        } catch (e: BackupException.UnsupportedVersion) {
            assertEquals(99, e.version)
        }
    }

    @Test
    fun `readEnvelope with truncated data throws exception`() {
        val truncatedBytes = byteArrayOf('P'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte())
        val bais = ByteArrayInputStream(truncatedBytes)

        try {
            BackupContainer.readEnvelope(bais)
            fail("Expected exception on truncated file")
        } catch (_: Exception) {
            // Success
        }
    }
}
