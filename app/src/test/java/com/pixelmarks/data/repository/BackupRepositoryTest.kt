package com.pixelmarks.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var repository: BackupRepository
    private lateinit var fakeBrowserRepo: FakeBrowserPreferenceRepository
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private class FakeBrowserPreferenceRepository : BrowserPreferenceRepository {
        var current = BrowserPreference.SYSTEM_PREFERRED
        override fun observePreference(): Flow<BrowserPreference> = flowOf(current)
        override suspend fun setPreference(preference: BrowserPreference) { current = preference }
        override suspend fun getPreference(): BrowserPreference = current
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            PixelMarksDatabase::class.java
        ).allowMainThreadQueries().build()

        fakeBrowserRepo = FakeBrowserPreferenceRepository()
        repository = BackupRepositoryImpl(context, database, fakeBrowserRepo)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `exportBackup generates encrypted file without plaintext secrets`() = runBlocking {
        // Seed database
        database.collectionDao().insert(
            CollectionEntity(id = 1, name = "Work", createdAt = 1000L, updatedAt = 1000L)
        )
        database.tagDao().insert(
            TagEntity(id = 1, name = "Android", createdAt = 1000L)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1,
                url = "https://developer.android.com",
                normalizedUrl = "https://developer.android.com",
                title = "Android Developers Official",
                domain = "developer.android.com",
                notes = "Confidential secret note 12345",
                createdAt = 1000L,
                updatedAt = 1000L,
                collectionId = 1
            )
        )

        val password = "StrongPassword2026!".toCharArray()
        val baos = ByteArrayOutputStream()
        val exportResult = repository.exportBackup(password, baos)

        val exportedBytes = baos.toByteArray()
        assertTrue(exportedBytes.isNotEmpty())
        assertEquals(1, exportResult.bookmarkCount)
        assertEquals(1, exportResult.collectionCount)
        assertEquals(1, exportResult.tagCount)

        // Verify that plaintext strings DO NOT appear anywhere in the raw exported bytes
        val rawContent = String(exportedBytes, Charsets.ISO_8859_1)
        assertFalse("Plaintext title leaked in backup", rawContent.contains("Android Developers Official"))
        assertFalse("Plaintext URL leaked in backup", rawContent.contains("https://developer.android.com"))
        assertFalse("Plaintext note leaked in backup", rawContent.contains("Confidential secret note 12345"))
    }

    @Test
    fun `restore with REPLACE mode successfully wipes current data and restores backup data`() = runBlocking {
        val password = "TestPassword123!".toCharArray()

        // 1. Seed initial data to backup
        database.collectionDao().insert(
            CollectionEntity(id = 1, name = "Tech", createdAt = 1000L, updatedAt = 1000L)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1,
                url = "https://kotlinlang.org",
                normalizedUrl = "https://kotlinlang.org",
                title = "Kotlin Programming Language",
                domain = "kotlinlang.org",
                createdAt = 1000L,
                updatedAt = 1000L,
                collectionId = 1
            )
        )

        // 2. Export backup
        val baos = ByteArrayOutputStream()
        repository.exportBackup(password, baos)
        val backupBytes = baos.toByteArray()

        // 3. Mutate current database (add unrelated bookmark, delete old)
        database.bookmarkDao().deleteAll()
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 99,
                url = "https://unrelated.com",
                normalizedUrl = "https://unrelated.com",
                title = "Unrelated Site",
                domain = "unrelated.com",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // 4. Decrypt and Restore with REPLACE mode
        val payload = repository.decryptAndValidate(password, ByteArrayInputStream(backupBytes))
        val restoreResult = repository.restoreBackup(payload, RestoreMode.REPLACE)

        assertEquals(1, restoreResult.bookmarksRestored)
        val currentBookmarks = database.bookmarkDao().getAll()
        assertEquals(1, currentBookmarks.size)
        assertEquals("Kotlin Programming Language", currentBookmarks[0].title)
        assertEquals("https://kotlinlang.org", currentBookmarks[0].url)
    }

    @Test
    fun `restore with MERGE mode keeps existing bookmarks and adds non-duplicate backup bookmarks`() = runBlocking {
        val password = "MergePassword123!".toCharArray()

        // 1. Create backup with bookmark A and B
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1,
                url = "https://siteA.com",
                normalizedUrl = "https://sitea.com",
                title = "Site A",
                domain = "siteA.com",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 2,
                url = "https://siteB.com",
                normalizedUrl = "https://siteb.com",
                title = "Site B",
                domain = "siteB.com",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val baos = ByteArrayOutputStream()
        repository.exportBackup(password, baos)
        val backupBytes = baos.toByteArray()

        // 2. In DB, retain siteA and add siteC
        database.bookmarkDao().deleteAll()
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1,
                url = "https://siteA.com",
                normalizedUrl = "https://sitea.com",
                title = "Site A Existing",
                domain = "siteA.com",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 3,
                url = "https://siteC.com",
                normalizedUrl = "https://sitec.com",
                title = "Site C",
                domain = "siteC.com",
                createdAt = 2000L,
                updatedAt = 2000L
            )
        )

        // 3. Restore with MERGE
        val payload = repository.decryptAndValidate(password, ByteArrayInputStream(backupBytes))
        val restoreResult = repository.restoreBackup(payload, RestoreMode.MERGE)

        // Site A was matched and not duplicated; Site B was added
        val allInDb = database.bookmarkDao().getAll()
        assertEquals(3, allInDb.size) // site A (existing), site C (existing), site B (merged from backup)
        val urls = allInDb.map { it.url }.toSet()
        assertTrue(urls.contains("https://siteA.com"))
        assertTrue(urls.contains("https://siteB.com"))
        assertTrue(urls.contains("https://siteC.com"))
    }

    @Test
    fun `decrypt with wrong password throws InvalidPasswordOrCorrupt and leaves database unchanged`() = runBlocking {
        val correctPassword = "CorrectPassword123!".toCharArray()
        val wrongPassword = "WrongPassword999!".toCharArray()

        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1,
                url = "https://original.com",
                normalizedUrl = "https://original.com",
                title = "Original Bookmark",
                domain = "original.com",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val baos = ByteArrayOutputStream()
        repository.exportBackup(correctPassword, baos)
        val backupBytes = baos.toByteArray()

        try {
            repository.decryptAndValidate(wrongPassword, ByteArrayInputStream(backupBytes))
            fail("Expected decryption failure with wrong password")
        } catch (_: BackupException.InvalidPasswordOrCorrupt) {
            // Success
        }

        // Database remains unaffected
        val inDb = database.bookmarkDao().getAll()
        assertEquals(1, inDb.size)
        assertEquals("Original Bookmark", inDb[0].title)
    }

    @Test
    fun `decrypt with tampered bytes throws exception and leaves database unchanged`() = runBlocking {
        val password = "ValidPassword123!".toCharArray()

        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1,
                url = "https://original.com",
                normalizedUrl = "https://original.com",
                title = "Original Bookmark",
                domain = "original.com",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val baos = ByteArrayOutputStream()
        repository.exportBackup(password, baos)
        val backupBytes = baos.toByteArray()

        // Flip bytes near the end of file (ciphertext area)
        backupBytes[backupBytes.size - 5] = (backupBytes[backupBytes.size - 5] + 1).toByte()

        try {
            repository.decryptAndValidate(password, ByteArrayInputStream(backupBytes))
            fail("Expected authentication failure on tampered backup")
        } catch (_: BackupException.InvalidPasswordOrCorrupt) {
            // Expected
        }

        // Database remains unaffected
        val inDb = database.bookmarkDao().getAll()
        assertEquals(1, inDb.size)
        assertEquals("Original Bookmark", inDb[0].title)
    }
}
