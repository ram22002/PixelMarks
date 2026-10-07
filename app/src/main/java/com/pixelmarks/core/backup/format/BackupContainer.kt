package com.pixelmarks.core.backup.format

import com.pixelmarks.core.backup.model.BACKUP_MAGIC_BYTES
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.CURRENT_BACKUP_FORMAT_VERSION
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

object BackupContainer {

    /**
     * Serializes the header components into an authenticated byte array (AAD).
     */
    fun createAad(header: BackupHeaderInfo): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.write(BACKUP_MAGIC_BYTES)
        dos.writeInt(header.formatVersion)
        dos.writeLong(header.createdAt)
        dos.writeInt(header.kdfIterations)
        dos.writeInt(header.salt.size)
        dos.write(header.salt)
        dos.writeInt(header.nonce.size)
        dos.write(header.nonce)
        dos.flush()
        return baos.toByteArray()
    }

    /**
     * Packs the complete .pmb envelope and writes it to an OutputStream.
     */
    fun writeEnvelope(
        outputStream: OutputStream,
        header: BackupHeaderInfo,
        ciphertext: ByteArray
    ) {
        val dos = DataOutputStream(outputStream)
        // 1. Magic
        dos.write(BACKUP_MAGIC_BYTES)
        // 2. Version
        dos.writeInt(header.formatVersion)
        // 3. CreatedAt
        dos.writeLong(header.createdAt)
        // 4. KDF Iterations
        dos.writeInt(header.kdfIterations)
        // 5. Salt
        dos.writeInt(header.salt.size)
        dos.write(header.salt)
        // 6. Nonce
        dos.writeInt(header.nonce.size)
        dos.write(header.nonce)
        // 7. Ciphertext
        dos.writeInt(ciphertext.size)
        dos.write(ciphertext)
        dos.flush()
    }

    /**
     * Reads and validates the container header from an InputStream without reading the full ciphertext.
     */
    fun readHeader(inputStream: InputStream): BackupHeaderInfo {
        val dis = DataInputStream(inputStream)

        // 1. Magic
        val magic = ByteArray(BACKUP_MAGIC_BYTES.size)
        dis.readFully(magic)
        if (!magic.contentEquals(BACKUP_MAGIC_BYTES)) {
            throw BackupException.InvalidFormat("Invalid backup file: magic header mismatch")
        }

        // 2. Version
        val version = dis.readInt()
        if (version < 1) {
            throw BackupException.InvalidFormat("Corrupted backup file: invalid version $version")
        }
        if (version > CURRENT_BACKUP_FORMAT_VERSION) {
            throw BackupException.UnsupportedVersion(version)
        }

        // 3. CreatedAt
        val createdAt = dis.readLong()

        // 4. KDF Iterations
        val iterations = dis.readInt()
        if (iterations <= 0) {
            throw BackupException.InvalidFormat("Corrupted backup file: invalid KDF iterations")
        }

        // 5. Salt
        val saltLen = dis.readInt()
        if (saltLen <= 0 || saltLen > 256) {
            throw BackupException.InvalidFormat("Corrupted backup file: invalid salt length")
        }
        val salt = ByteArray(saltLen)
        dis.readFully(salt)

        // 6. Nonce
        val nonceLen = dis.readInt()
        if (nonceLen <= 0 || nonceLen > 128) {
            throw BackupException.InvalidFormat("Corrupted backup file: invalid nonce length")
        }
        val nonce = ByteArray(nonceLen)
        dis.readFully(nonce)

        return BackupHeaderInfo(
            formatVersion = version,
            createdAt = createdAt,
            kdfIterations = iterations,
            salt = salt,
            nonce = nonce
        )
    }

    /**
     * Reads the entire .pmb file from an InputStream, returning the header info and ciphertext bytes.
     */
    fun readEnvelope(inputStream: InputStream): Pair<BackupHeaderInfo, ByteArray> {
        val dis = DataInputStream(inputStream)
        val header = readHeader(dis)

        // 7. Ciphertext
        val ciphertextLen = dis.readInt()
        if (ciphertextLen <= 0) {
            throw BackupException.CorruptedFile("Backup file contains no encrypted data")
        }
        val ciphertext = ByteArray(ciphertextLen)
        dis.readFully(ciphertext)

        return Pair(header, ciphertext)
    }
}
