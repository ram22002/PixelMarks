package com.pixelmarks.data.repository

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.backup.crypto.AutoBackupKeyManager
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.BACKUP_MAGIC_BYTES
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveConnectionState
import com.pixelmarks.core.backup.model.DriveException
import com.pixelmarks.core.backup.model.GoogleAccountInfo
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.database.model.BackupType
import com.pixelmarks.data.backup.GoogleAuthManager
import com.pixelmarks.data.backup.GoogleDriveClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class FakeGoogleAuthManager : GoogleAuthManager {
    var fakeAccount: GoogleAccountInfo? = null
    var shouldExpireToken = false
    var signInIntentResult: Result<GoogleAccountInfo> = Result.success(GoogleAccountInfo("user@example.com", "Test User"))

    override fun getConnectedAccount(): GoogleAccountInfo? = fakeAccount

    override suspend fun getAccessToken(): String {
        if (fakeAccount == null) throw DriveException.NotConnected()
        if (shouldExpireToken) throw DriveException.AuthExpired()
        return "valid_test_token"
    }

    override fun getSignInIntent(): Intent = Intent("fake.action.SIGN_IN")

    override fun handleSignInResult(data: Intent?): Result<GoogleAccountInfo> {
        if (signInIntentResult.isSuccess) {
            fakeAccount = signInIntentResult.getOrNull()
        }
        return signInIntentResult
    }

    override suspend fun signOut() {
        fakeAccount = null
    }

    override suspend fun disconnect() {
        fakeAccount = null
    }
}

class FakeGoogleDriveClient : GoogleDriveClient {
    val cloudFiles = mutableMapOf<String, Pair<DriveBackupFile, ByteArray>>()
    var shouldFailUpload = false
    var shouldFailDownload = false

    override suspend fun listBackups(accessToken: String): List<DriveBackupFile> {
        return cloudFiles.values.map { it.first }.sortedByDescending { it.createdAt }
    }

    override suspend fun uploadBackup(
        accessToken: String,
        filename: String,
        inputStream: InputStream,
        sizeBytes: Long
    ): DriveBackupFile {
        if (shouldFailUpload) throw DriveException.UploadFailed("Simulated upload failure")
        val id = "drive_file_${UUID.randomUUID()}"
        val bytes = inputStream.readBytes()
        val now = System.currentTimeMillis()
        val fileInfo = DriveBackupFile(
            id = id,
            name = filename,
            sizeBytes = bytes.size.toLong(),
            createdAt = now,
            modifiedAt = now
        )
        cloudFiles[id] = Pair(fileInfo, bytes)
        return fileInfo
    }

    override suspend fun downloadBackup(accessToken: String, fileId: String, outputStream: OutputStream) {
        if (shouldFailDownload) throw DriveException.DownloadFailed("Simulated download failure")
        val entry = cloudFiles[fileId] ?: throw DriveException.FileNotFound()
        outputStream.write(entry.second)
        outputStream.flush()
    }

    override suspend fun deleteBackup(accessToken: String, fileId: String) {
        cloudFiles.remove(fileId)
    }
}

@RunWith(RobolectricTestRunner::class)
class DriveBackupRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: PixelMarksDatabase
    private lateinit var backupRepository: BackupRepositoryImpl
    private lateinit var browserPreferenceRepository: BrowserPreferenceRepository
    private lateinit var drivePreferencesRepository: DrivePreferencesRepositoryImpl
    private lateinit var autoBackupKeyManager: AutoBackupKeyManager
    private lateinit var fakeAuthManager: FakeGoogleAuthManager
    private lateinit var fakeDriveClient: FakeGoogleDriveClient
    private lateinit var repository: DriveBackupRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)
        database = PixelMarksDatabase.buildInMemory(context)
        browserPreferenceRepository = BrowserPreferenceRepositoryImpl(context)
        backupRepository = BackupRepositoryImpl(context, database, browserPreferenceRepository)
        drivePreferencesRepository = DrivePreferencesRepositoryImpl(context)
        drivePreferencesRepository.clear()
        autoBackupKeyManager = AutoBackupKeyManager(context)
        autoBackupKeyManager.clearAutoBackupPassword()

        fakeAuthManager = FakeGoogleAuthManager()
        fakeDriveClient = FakeGoogleDriveClient()

        repository = DriveBackupRepositoryImpl(
            context = context,
            database = database,
            backupRepository = backupRepository,
            googleAuthManager = fakeAuthManager,
            googleDriveClient = fakeDriveClient,
            drivePreferencesRepository = drivePreferencesRepository,
            autoBackupKeyManager = autoBackupKeyManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun connectAndDisconnect_stateTransitions() = runBlocking {
        assertEquals(DriveConnectionState.DISCONNECTED, repository.connectionState.value)
        assertNull(repository.connectedAccount.value)

        // Connect
        val result = repository.handleSignInResult(Intent())
        assertTrue(result.isSuccess)
        assertEquals(DriveConnectionState.CONNECTED, repository.connectionState.value)
        assertEquals("user@example.com", repository.connectedAccount.value?.email)

        // Disconnect
        repository.disconnect()
        assertEquals(DriveConnectionState.DISCONNECTED, repository.connectionState.value)
        assertNull(repository.connectedAccount.value)
    }

    @Test
    fun uploadBackup_encryptsPmbAndSavesRecord() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect

        // Seed database with bookmarks (including private ones)
        val now = System.currentTimeMillis()
        val colId = database.collectionDao().insert(
            CollectionEntity(name = "Cloud Col", icon = "cloud", isPrivate = false, createdAt = now, updatedAt = now)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://example.com/public",
                normalizedUrl = "https://example.com/public",
                domain = "example.com",
                title = "Public Link",
                collectionId = colId,
                isPrivate = false,
                createdAt = now,
                updatedAt = now
            )
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://example.com/secret",
                normalizedUrl = "https://example.com/secret",
                domain = "example.com",
                title = "Secret Link",
                isPrivate = true,
                createdAt = now,
                updatedAt = now
            )
        )

        // Upload
        val password = "MySecurePassword123!".toCharArray()
        val uploaded = repository.uploadBackup(password)

        assertNotNull(uploaded.id)
        assertTrue(uploaded.name.endsWith(".pmb"))
        assertTrue(uploaded.sizeBytes > 0)

        // Verify stored in cloud files
        val storedFile = fakeDriveClient.cloudFiles[uploaded.id]
        assertNotNull(storedFile)
        val fileBytes = storedFile!!.second
        // Verify .pmb magic bytes PMBK
        assertEquals(BACKUP_MAGIC_BYTES[0], fileBytes[0])
        assertEquals(BACKUP_MAGIC_BYTES[1], fileBytes[1])
        assertEquals(BACKUP_MAGIC_BYTES[2], fileBytes[2])
        assertEquals(BACKUP_MAGIC_BYTES[3], fileBytes[3])

        // Verify database backup record created with GOOGLE_DRIVE type
        val record = database.backupRecordDao().getLatest()
        assertNotNull(record)
        assertEquals(BackupType.GOOGLE_DRIVE, record?.type)
    }

    @Test
    fun uploadBackup_authExpired_updatesStateToAuthExpired() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect
        fakeAuthManager.shouldExpireToken = true

        try {
            repository.uploadBackup("MyPassword123!".toCharArray())
            fail("Expected DriveException.AuthExpired")
        } catch (_: DriveException.AuthExpired) {
            assertEquals(DriveConnectionState.AUTH_EXPIRED, repository.connectionState.value)
        }
    }

    @Test
    fun downloadInspectAndRestore_replaceMode_success() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect

        // Seed original data
        val now = System.currentTimeMillis()
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://original.com",
                normalizedUrl = "https://original.com",
                domain = "original.com",
                title = "Original Title",
                createdAt = now,
                updatedAt = now
            )
        )

        val password = "TestPassword456!".toCharArray()
        val uploaded = repository.uploadBackup(password)

        // Now modify local database
        database.bookmarkDao().deleteAll()
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://different.com",
                normalizedUrl = "https://different.com",
                domain = "different.com",
                title = "Different Title",
                createdAt = now,
                updatedAt = now
            )
        )
        assertEquals(1, database.bookmarkDao().getAll().size)
        assertEquals("Different Title", database.bookmarkDao().getAll().first().title)

        // Download & inspect cloud backup
        val (tempFile, header) = repository.downloadAndInspectBackup(uploaded.id, context.cacheDir)
        assertNotNull(header)
        assertTrue(tempFile.exists())

        // Restore REPLACE mode
        val restoreResult = repository.restoreCloudBackup(tempFile, "TestPassword456!".toCharArray(), RestoreMode.REPLACE)
        assertEquals(1, restoreResult.bookmarksRestored)
        assertEquals(RestoreMode.REPLACE, restoreResult.mode)

        // Database should now have the original data back
        val currentBookmarks = database.bookmarkDao().getAll()
        assertEquals(1, currentBookmarks.size)
        assertEquals("Original Title", currentBookmarks.first().title)
    }

    @Test
    fun restoreCloudBackup_wrongPassword_rejectsAndPreservesOriginalData() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect

        val now = System.currentTimeMillis()
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://important.com",
                normalizedUrl = "https://important.com",
                domain = "important.com",
                title = "Important Local Data",
                createdAt = now,
                updatedAt = now
            )
        )

        // Upload valid backup
        val correctPassword = "CorrectPassword123!".toCharArray()
        val uploaded = repository.uploadBackup(correctPassword)

        val (tempFile, _) = repository.downloadAndInspectBackup(uploaded.id, context.cacheDir)

        // Try to restore with wrong password
        try {
            repository.restoreCloudBackup(tempFile, "WrongPassword999!".toCharArray(), RestoreMode.REPLACE)
            fail("Expected BackupException.InvalidPasswordOrCorrupt")
        } catch (e: BackupException.InvalidPasswordOrCorrupt) {
            // Success
        }

        // Local data MUST be completely intact
        val bookmarks = database.bookmarkDao().getAll()
        assertEquals(1, bookmarks.size)
        assertEquals("Important Local Data", bookmarks.first().title)
    }

    @Test
    fun restoreCloudBackup_corruptedFile_rejectsAndPreservesOriginalData() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect

        val now = System.currentTimeMillis()
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://safe.com",
                normalizedUrl = "https://safe.com",
                domain = "safe.com",
                title = "Safe Data",
                createdAt = now,
                updatedAt = now
            )
        )

        val corruptedFile = File(context.cacheDir, "corrupted.pmb")
        corruptedFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5)) // invalid header

        try {
            repository.restoreCloudBackup(corruptedFile, "SomePassword123!".toCharArray(), RestoreMode.REPLACE)
            fail("Expected BackupException")
        } catch (_: BackupException) {
            // Expected
        }

        // Local data preserved
        assertEquals(1, database.bookmarkDao().getAll().size)
        assertEquals("Safe Data", database.bookmarkDao().getAll().first().title)
    }

    @Test
    fun deleteBackup_removesFromDrive() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect
        val uploaded = repository.uploadBackup("Password123!".toCharArray())
        assertEquals(1, fakeDriveClient.cloudFiles.size)

        repository.deleteBackup(uploaded.id)
        assertEquals(0, fakeDriveClient.cloudFiles.size)
    }

    @Test
    fun autoBackupPreferences_and_keyManager_noPlaintextStorage() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect

        // Configure auto backup
        val password = "AutoPassword123!".toCharArray()
        repository.setAutoBackupEnabled(true, password)

        val prefs = repository.autoBackupPreferences.first()
        assertTrue(prefs.enabled)

        // KeyStore should have password
        assertTrue(autoBackupKeyManager.hasAutoBackupPassword())
        val retrieved = autoBackupKeyManager.getAutoBackupPassword()
        assertNotNull(retrieved)
        assertEquals("AutoPassword123!", String(retrieved!!))

        // Update preferences
        repository.setAutoBackupFrequency(AutoBackupFrequency.WEEKLY)
        repository.setWifiOnly(false)

        val updatedPrefs = repository.autoBackupPreferences.first()
        assertEquals(AutoBackupFrequency.WEEKLY, updatedPrefs.frequency)
        assertFalse(updatedPrefs.wifiOnly)

        // Disable auto backup
        repository.setAutoBackupEnabled(false)
        assertFalse(repository.autoBackupPreferences.first().enabled)
        assertFalse(autoBackupKeyManager.hasAutoBackupPassword())
    }

    @Test
    fun privateItemsPreservedThroughCloudBackup() = runBlocking {
        repository.handleSignInResult(Intent()) // Connect

        val now = System.currentTimeMillis()
        val privateColId = database.collectionDao().insert(
            CollectionEntity(name = "Secret Col", isPrivate = true, createdAt = now, updatedAt = now)
        )
        val privateTagId = database.tagDao().insert(
            TagEntity(name = "Secret Tag", isPrivate = true, createdAt = now)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://private.example.com",
                normalizedUrl = "https://private.example.com",
                domain = "private.example.com",
                title = "Private Item",
                collectionId = privateColId,
                isPrivate = true,
                createdAt = now,
                updatedAt = now
            )
        )

        // Upload and wipe database
        val uploaded = repository.uploadBackup("Pass1234!".toCharArray())
        database.bookmarkDao().deleteAll()
        database.collectionDao().deleteAll()
        database.tagDao().deleteAll()

        // Restore
        val (tempFile, _) = repository.downloadAndInspectBackup(uploaded.id, context.cacheDir)
        repository.restoreCloudBackup(tempFile, "Pass1234!".toCharArray(), RestoreMode.REPLACE)

        // Verify privacy flags intact
        val restoredBookmarks = database.bookmarkDao().getAll()
        val restoredCollections = database.collectionDao().getAll()
        val restoredTags = database.tagDao().getAll()

        assertEquals(1, restoredBookmarks.size)
        assertTrue(restoredBookmarks.first().isPrivate)
        assertEquals("Private Item", restoredBookmarks.first().title)

        assertEquals(1, restoredCollections.size)
        assertTrue(restoredCollections.first().isPrivate)

        assertEquals(1, restoredTags.size)
        assertTrue(restoredTags.first().isPrivate)
    }

    @Test
    fun signInCancelledOrFailed_setsStateDisconnected() = runBlocking {
        fakeAuthManager.signInIntentResult = Result.failure(DriveException.NetworkError("Google sign-in was cancelled"))
        val result = repository.handleSignInResult(null)

        assertTrue(result.isFailure)
        assertEquals(DriveConnectionState.DISCONNECTED, repository.connectionState.value)
        assertNull(repository.connectedAccount.value)
    }

    @Test
    fun uploadWithRevokedToken_throwsAuthExpiredAndUpdatesConnectionState() = runBlocking {
        repository.handleSignInResult(Intent())
        assertEquals(DriveConnectionState.CONNECTED, repository.connectionState.value)

        fakeAuthManager.shouldExpireToken = true

        try {
            repository.uploadBackup("Pass1234!".toCharArray())
            fail("Expected DriveException.AuthExpired")
        } catch (_: DriveException.AuthExpired) {
            assertEquals(DriveConnectionState.AUTH_EXPIRED, repository.connectionState.value)
        }
    }
}
