package com.pixelmarks.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.data.repository.BackupRepository
import com.pixelmarks.data.repository.BackupRepositoryImpl
import com.pixelmarks.data.repository.BrowserPreferenceRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupPrivacyTest {

    private lateinit var context: Context
    private lateinit var db: PixelMarksDatabase
    private lateinit var backupRepository: BackupRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = PixelMarksDatabase.buildInMemory(context)
        val browserRepo = BrowserPreferenceRepositoryImpl(context)
        backupRepository = BackupRepositoryImpl(context, db, browserRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun backupExport_preservesLockedAndHiddenFlagsInPayload() = runBlocking {
        val now = System.currentTimeMillis()
        val colDao = db.collectionDao()

        colDao.insert(
            CollectionEntity(
                name = "Private Vault",
                icon = "folder",
                color = "#EF4444",
                isLocked = true,
                isHidden = true,
                createdAt = now,
                updatedAt = now
            )
        )

        val payload = backupRepository.createBackupPayload()
        assertEquals(1, payload.collections.size)
        val colDto = payload.collections.first()
        assertTrue(colDto.isLocked)
        assertTrue(colDto.isHidden)
    }

    @Test
    fun backupRestore_restoresPrivacyFlagsAccurately() = runBlocking {
        val now = System.currentTimeMillis()
        val colDao = db.collectionDao()
        val bmDao = db.bookmarkDao()

        val colId = colDao.insert(
            CollectionEntity(
                name = "Secure Vault",
                icon = "folder",
                color = "#8B5CF6",
                isLocked = true,
                isHidden = true,
                createdAt = now,
                updatedAt = now
            )
        )

        bmDao.insert(
            BookmarkEntity(
                url = "https://private.vault/doc",
                normalizedUrl = "https://private.vault/doc",
                domain = "private.vault",
                title = "Confidential Plan",
                collectionId = colId,
                createdAt = now,
                updatedAt = now
            )
        )

        val password = "StrongPassword2026!".toCharArray()
        val baos = ByteArrayOutputStream()
        backupRepository.exportBackup(password, baos)
        val backupBytes = baos.toByteArray()

        // Clear current tables before restore
        bmDao.deleteAll()
        colDao.getAll().forEach { colDao.deleteById(it.id) }
        assertEquals(0, colDao.getAll().size)
        assertEquals(0, bmDao.getAll().size)

        // Decrypt and Restore
        val payload = backupRepository.decryptAndValidate(password, ByteArrayInputStream(backupBytes))
        val restoreResult = backupRepository.restoreBackup(payload, RestoreMode.REPLACE)

        assertEquals(1, restoreResult.collectionsRestored)
        assertEquals(1, restoreResult.bookmarksRestored)

        // Verify collection privacy flags are intact
        val restoredCols = colDao.getAll()
        assertEquals(1, restoredCols.size)
        val restoredCol = restoredCols.first()
        assertEquals("Secure Vault", restoredCol.name)
        assertTrue(restoredCol.isLocked)
        assertTrue(restoredCol.isHidden)

        // Verify bookmark relationship is preserved
        val restoredBms = bmDao.getAll()
        assertEquals(1, restoredBms.size)
        assertEquals(restoredCol.id, restoredBms.first().collectionId)
    }
}
