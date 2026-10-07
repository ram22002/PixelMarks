package com.pixelmarks.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.database.dao.CollectionDao
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.CollectionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CollectionPrivacyTest {

    private lateinit var db: PixelMarksDatabase
    private lateinit var collectionDao: CollectionDao
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = PixelMarksDatabase.buildInMemory(context)
        collectionDao = db.collectionDao()
        bookmarkDao = db.bookmarkDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun lockedCollectionState_persistedAndRetrieved() = runBlocking {
        val now = System.currentTimeMillis()
        val entity = CollectionEntity(
            name = "Work Secrets",
            icon = "folder",
            color = "#EF4444",
            isLocked = true,
            isHidden = false,
            createdAt = now,
            updatedAt = now
        )
        val colId = collectionDao.insert(entity)

        val retrieved = collectionDao.getById(colId)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isLocked)
        assertFalse(retrieved.isHidden)
    }

    @Test
    fun hiddenCollectionState_persistedAndRetrieved() = runBlocking {
        val now = System.currentTimeMillis()
        val entity = CollectionEntity(
            name = "Top Secret",
            icon = "folder",
            color = "#10B981",
            isLocked = true,
            isHidden = true,
            isPrivate = true,
            createdAt = now,
            updatedAt = now
        )
        val colId = collectionDao.insert(entity)

        val retrieved = collectionDao.getById(colId)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isPrivate)

        // Excluded from observeRootCollections unless isPrivate is true
        val normalRoots = collectionDao.observeRootCollections(isPrivate = false).first()
        assertFalse(normalRoots.any { it.id == colId })

        val privateRoots = collectionDao.observeRootCollections(isPrivate = true).first()
        assertTrue(privateRoots.any { it.id == colId })
    }

    @Test
    fun collectionPrivacyState_survivesUpdates() = runBlocking {
        val now = System.currentTimeMillis()
        val entity = CollectionEntity(
            name = "Regular Collection",
            icon = "folder",
            color = "#3B82F6",
            isLocked = false,
            isHidden = false,
            createdAt = now,
            updatedAt = now
        )
        val colId = collectionDao.insert(entity)

        // Update to locked & hidden
        collectionDao.update(
            entity.copy(
                id = colId,
                isLocked = true,
                isHidden = true,
                updatedAt = now + 1000L
            )
        )

        val updated = collectionDao.getById(colId)
        assertNotNull(updated)
        assertTrue(updated!!.isLocked)
        assertTrue(updated.isHidden)
    }

    @Test
    fun deleteCollection_doesNotDeleteBookmarksOrLeak() = runBlocking {
        val now = System.currentTimeMillis()
        val col = CollectionEntity(
            name = "Private Vault",
            icon = "folder",
            color = "#3B82F6",
            isLocked = true,
            isHidden = true,
            createdAt = now,
            updatedAt = now
        )
        val colId = collectionDao.insert(col)

        val bookmark = BookmarkEntity(
            url = "https://private.vault.test/credentials",
            normalizedUrl = "https://private.vault.test/credentials",
            domain = "private.vault.test",
            title = "Confidential Credentials",
            collectionId = colId,
            createdAt = now,
            updatedAt = now
        )
        val bmId = bookmarkDao.insert(bookmark)

        // Delete collection - SQLite foreign key automatically resets collectionId to null
        collectionDao.deleteById(colId)

        val retrievedBm = bookmarkDao.getById(bmId)
        assertNotNull(retrievedBm)
        assertNull(retrievedBm!!.collectionId)
        assertEquals("Confidential Credentials", retrievedBm.title)
    }

    @Test
    fun observeByCollection_strictlyFiltersByPrivacy() = runBlocking {
        val now = System.currentTimeMillis()
        val col = CollectionEntity(
            name = "Test Vault",
            icon = "folder",
            color = "#3B82F6",
            isPrivate = true,
            createdAt = now,
            updatedAt = now
        )
        val colId = collectionDao.insert(col)

        val privateBm = BookmarkEntity(
            url = "https://private.vault.test/1",
            normalizedUrl = "https://private.vault.test/1",
            domain = "private.vault.test",
            title = "Secret Item",
            collectionId = colId,
            isPrivate = true,
            createdAt = now,
            updatedAt = now
        )
        val publicBm = BookmarkEntity(
            url = "https://public.vault.test/2",
            normalizedUrl = "https://public.vault.test/2",
            domain = "public.vault.test",
            title = "Public Item",
            collectionId = colId,
            isPrivate = false,
            createdAt = now + 1,
            updatedAt = now + 1
        )
        bookmarkDao.insert(privateBm)
        bookmarkDao.insert(publicBm)

        val inPublicMode = bookmarkDao.observeByCollection(colId, isPrivate = false).first()
        assertEquals(1, inPublicMode.size)
        assertEquals("Public Item", inPublicMode[0].title)

        val inPrivateMode = bookmarkDao.observeByCollection(colId, isPrivate = true).first()
        assertEquals(1, inPrivateMode.size)
        assertEquals("Secret Item", inPrivateMode[0].title)
    }
}
