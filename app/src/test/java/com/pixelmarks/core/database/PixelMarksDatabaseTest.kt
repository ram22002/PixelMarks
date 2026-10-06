package com.pixelmarks.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.dao.BackupRecordDao
import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.database.dao.BookmarkTagDao
import com.pixelmarks.core.database.dao.CollectionDao
import com.pixelmarks.core.database.dao.ConnectedDeviceDao
import com.pixelmarks.core.database.dao.ReminderDao
import com.pixelmarks.core.database.dao.TagDao
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType
import com.pixelmarks.core.database.model.DeviceType
import com.pixelmarks.core.database.model.MetadataStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PixelMarksDatabaseTest {

    private lateinit var db: PixelMarksDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var collectionDao: CollectionDao
    private lateinit var tagDao: TagDao
    private lateinit var bookmarkTagDao: BookmarkTagDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var connectedDeviceDao: ConnectedDeviceDao
    private lateinit var backupRecordDao: BackupRecordDao
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = PixelMarksDatabase.buildInMemory(context)
        bookmarkDao = db.bookmarkDao()
        collectionDao = db.collectionDao()
        tagDao = db.tagDao()
        bookmarkTagDao = db.bookmarkTagDao()
        reminderDao = db.reminderDao()
        connectedDeviceDao = db.connectedDeviceDao()
        backupRecordDao = db.backupRecordDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testBookmarkCrudAndNormalizedUrlLookup() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark = BookmarkEntity(
            url = "https://example.com/article?utm_source=twitter",
            normalizedUrl = "https://example.com/article",
            title = "Example Article",
            domain = "example.com",
            createdAt = now,
            updatedAt = now
        )

        val id = bookmarkDao.insert(bookmark)
        assertTrue(id > 0)

        // Retrieve
        val retrieved = bookmarkDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("Example Article", retrieved?.title)
        assertEquals("https://example.com/article", retrieved?.normalizedUrl)

        // Update
        val updated = retrieved!!.copy(title = "Updated Title", isFavorite = true)
        bookmarkDao.update(updated)
        val afterUpdate = bookmarkDao.getById(id)
        assertEquals("Updated Title", afterUpdate?.title)
        assertTrue(afterUpdate?.isFavorite == true)

        // Normalized URL lookup
        val matchingList = bookmarkDao.getByNormalizedUrl("https://example.com/article")
        assertEquals(1, matchingList.size)
        assertEquals(id, matchingList.first().id)

        // Delete
        bookmarkDao.deleteById(id)
        assertNull(bookmarkDao.getById(id))
    }

    @Test
    fun testDuplicateNormalizedUrlsAllowed() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark1 = BookmarkEntity(
            url = "https://example.com/page",
            normalizedUrl = "https://example.com/page",
            title = "Page First Copy",
            domain = "example.com",
            createdAt = now,
            updatedAt = now
        )
        val bookmark2 = BookmarkEntity(
            url = "https://example.com/page#section2",
            normalizedUrl = "https://example.com/page",
            title = "Page Second Copy",
            domain = "example.com",
            createdAt = now + 1000,
            updatedAt = now + 1000
        )

        val id1 = bookmarkDao.insert(bookmark1)
        val id2 = bookmarkDao.insert(bookmark2)

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        assertTrue(id1 != id2)

        val matches = bookmarkDao.getByNormalizedUrl("https://example.com/page")
        assertEquals(2, matches.size)
    }

    @Test
    fun testCollectionHierarchyAndDeletionPreservation() = runBlocking {
        val now = System.currentTimeMillis()
        val parentCol = CollectionEntity(
            name = "Development",
            createdAt = now,
            updatedAt = now
        )
        val parentId = collectionDao.insert(parentCol)

        val childCol = CollectionEntity(
            name = "Android",
            parentId = parentId,
            createdAt = now,
            updatedAt = now
        )
        val childId = collectionDao.insert(childCol)

        // Verify child collection
        val children = collectionDao.observeChildren(parentId).first()
        assertEquals(1, children.size)
        assertEquals("Android", children.first().name)

        // Associate bookmark with child collection
        val bookmark = BookmarkEntity(
            url = "https://developer.android.com",
            normalizedUrl = "https://developer.android.com",
            title = "Android Developers",
            domain = "developer.android.com",
            collectionId = childId,
            createdAt = now,
            updatedAt = now
        )
        val bookmarkId = bookmarkDao.insert(bookmark)

        // Deleting collection must NOT delete the bookmark (SET NULL semantics)
        collectionDao.deleteById(childId)

        val preservedBookmark = bookmarkDao.getById(bookmarkId)
        assertNotNull("Bookmark must survive collection deletion", preservedBookmark)
        assertNull("Bookmark collectionId must be set to NULL after collection deletion", preservedBookmark?.collectionId)
    }

    @Test
    fun testTagRelationshipsAndTagDeletion() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark = BookmarkEntity(
            url = "https://kotlinlang.org",
            normalizedUrl = "https://kotlinlang.org",
            title = "Kotlin Programming Language",
            domain = "kotlinlang.org",
            createdAt = now,
            updatedAt = now
        )
        val bookmarkId = bookmarkDao.insert(bookmark)

        val tag = TagEntity(
            name = "Kotlin",
            color = "#7F52FF",
            createdAt = now
        )
        val tagId = tagDao.insert(tag)

        // Assign tag
        bookmarkTagDao.insert(BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagId))

        // Retrieve tags for bookmark
        val tags = bookmarkTagDao.getTagsForBookmark(bookmarkId)
        assertEquals(1, tags.size)
        assertEquals("Kotlin", tags.first().name)

        // Retrieve bookmarks for tag
        val bookmarks = bookmarkTagDao.getBookmarksForTag(tagId)
        assertEquals(1, bookmarks.size)
        assertEquals(bookmarkId, bookmarks.first().id)

        // Delete tag -> cross-reference is cascade deleted, bookmark survives
        tagDao.deleteById(tagId)

        val remainingTags = bookmarkTagDao.getTagsForBookmark(bookmarkId)
        assertTrue(remainingTags.isEmpty())

        val survivingBookmark = bookmarkDao.getById(bookmarkId)
        assertNotNull("Bookmark must survive tag deletion", survivingBookmark)
    }

    @Test
    fun testBookmarkDeletionCleanup() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark = BookmarkEntity(
            url = "https://news.ycombinator.com",
            normalizedUrl = "https://news.ycombinator.com",
            title = "Hacker News",
            domain = "news.ycombinator.com",
            createdAt = now,
            updatedAt = now
        )
        val bookmarkId = bookmarkDao.insert(bookmark)

        val tag = TagEntity(name = "News", createdAt = now)
        val tagId = tagDao.insert(tag)
        bookmarkTagDao.insert(BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagId))

        val reminder = ReminderEntity(
            bookmarkId = bookmarkId,
            reminderAt = now + 86400000,
            createdAt = now,
            updatedAt = now
        )
        val reminderId = reminderDao.insert(reminder)

        // Verify reminder created
        assertNotNull(reminderDao.getById(reminderId))

        // Delete bookmark -> cross-ref and reminder must cascade delete
        bookmarkDao.deleteById(bookmarkId)

        assertNull("Bookmark must be deleted", bookmarkDao.getById(bookmarkId))
        assertNull("Reminder must be cascade deleted when parent bookmark is deleted", reminderDao.getById(reminderId))
        val bookmarksForTag = bookmarkTagDao.getBookmarksForTag(tagId)
        assertTrue("Cross-ref must be cleaned up when bookmark is deleted", bookmarksForTag.isEmpty())
        assertNotNull("Tag itself must remain intact", tagDao.getById(tagId))
    }

    @Test
    fun testReminderDaoOperations() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark = BookmarkEntity(
            url = "https://docs.github.com",
            normalizedUrl = "https://docs.github.com",
            title = "GitHub Docs",
            domain = "docs.github.com",
            createdAt = now,
            updatedAt = now
        )
        val bookmarkId = bookmarkDao.insert(bookmark)

        val reminder = ReminderEntity(
            bookmarkId = bookmarkId,
            reminderAt = now + 3600000,
            createdAt = now,
            updatedAt = now,
            isCompleted = false
        )
        val reminderId = reminderDao.insert(reminder)

        val byBookmark = reminderDao.getByBookmarkId(bookmarkId)
        assertNotNull(byBookmark)
        assertEquals(reminderId, byBookmark?.id)

        // Mark completed
        reminderDao.update(byBookmark!!.copy(isCompleted = true))
        val completedList = reminderDao.observeCompleted().first()
        assertEquals(1, completedList.size)
        assertEquals(reminderId, completedList.first().id)
    }

    @Test
    fun testConnectedDeviceDaoOperations() = runBlocking {
        val now = System.currentTimeMillis()
        val device = ConnectedDeviceEntity(
            deviceName = "Chrome on macOS",
            deviceType = DeviceType.BROWSER,
            tokenHash = "sha256_mock_hash_value_12345",
            createdAt = now,
            lastSeenAt = now
        )
        val id = connectedDeviceDao.insert(device)
        assertTrue(id > 0)

        val retrieved = connectedDeviceDao.getByTokenHash("sha256_mock_hash_value_12345")
        assertNotNull(retrieved)
        assertEquals("Chrome on macOS", retrieved?.deviceName)

        val active = connectedDeviceDao.getActiveDevices()
        assertEquals(1, active.size)

        // Block device
        connectedDeviceDao.update(retrieved!!.copy(isBlocked = true))
        val activeAfterBlock = connectedDeviceDao.getActiveDevices()
        assertTrue(activeAfterBlock.isEmpty())
    }

    @Test
    fun testBackupRecordDaoOperations() = runBlocking {
        val now = System.currentTimeMillis()
        val record = BackupRecordEntity(
            type = BackupType.LOCAL,
            location = "/storage/emulated/0/Download/backup.pmb",
            createdAt = now,
            sizeBytes = 1048576,
            checksum = "sha256_mock_checksum",
            status = BackupStatus.SUCCESS
        )
        val id = backupRecordDao.insert(record)
        assertTrue(id > 0)

        val latest = backupRecordDao.getLatest()
        assertNotNull(latest)
        assertEquals(BackupType.LOCAL, latest?.type)
        assertEquals(BackupStatus.SUCCESS, latest?.status)
    }

    @Test
    fun testSingleDatabaseInstance() {
        val instance1 = PixelMarksDatabase.getInstance(context)
        val instance2 = PixelMarksDatabase.getInstance(context)
        assertSame("PixelMarksDatabase.getInstance must return the singleton instance", instance1, instance2)
    }
}
