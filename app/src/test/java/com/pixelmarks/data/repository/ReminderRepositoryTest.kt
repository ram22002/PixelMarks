package com.pixelmarks.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.model.Bookmark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderRepositoryTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var reminderRepository: ReminderRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())
        reminderRepository = ReminderRepositoryImpl(database.reminderDao(), bookmarkRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertTestBookmark(): Long {
        val now = System.currentTimeMillis()
        val bookmark = Bookmark(
            url = "https://example.com/test",
            normalizedUrl = "https://example.com/test",
            title = "Test Bookmark",
            domain = "example.com",
            createdAt = now,
            updatedAt = now
        )
        return bookmarkRepository.insert(bookmark)
    }

    @Test
    fun testSetReminder_syncsWithBookmark() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val reminderTime = System.currentTimeMillis() + 3600000L

        val reminderId = reminderRepository.setReminder(bookmarkId, reminderTime)
        assertTrue(reminderId > 0)

        val retrievedReminder = reminderRepository.getByBookmarkId(bookmarkId)
        assertNotNull(retrievedReminder)
        assertEquals(reminderTime, retrievedReminder?.reminderAt)

        val updatedBookmark = bookmarkRepository.getById(bookmarkId)
        assertEquals(reminderTime, updatedBookmark?.reminderAt)
    }

    @Test
    fun testObserveUpcomingReminders() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 7200000L
        reminderRepository.setReminder(bookmarkId, futureTime)

        val upcoming = reminderRepository.observeUpcoming(System.currentTimeMillis()).first()
        assertEquals(1, upcoming.size)
        assertEquals(bookmarkId, upcoming.first().bookmarkId)
    }

    @Test
    fun testMarkCompleted() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 3600000L
        val reminderId = reminderRepository.setReminder(bookmarkId, futureTime)

        reminderRepository.markCompleted(reminderId, isCompleted = true)

        val updatedReminder = reminderRepository.getById(reminderId)
        assertTrue(updatedReminder?.isCompleted == true)

        val updatedBookmark = bookmarkRepository.getById(bookmarkId)
        assertNull(updatedBookmark?.reminderAt)
    }

    @Test
    fun testCancelReminder() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 3600000L
        reminderRepository.setReminder(bookmarkId, futureTime)

        reminderRepository.cancelReminder(bookmarkId)

        assertNull(reminderRepository.getByBookmarkId(bookmarkId))
        val updatedBookmark = bookmarkRepository.getById(bookmarkId)
        assertNull(updatedBookmark?.reminderAt)
    }

    @Test
    fun testCascadeDeleteWithBookmark() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 3600000L
        reminderRepository.setReminder(bookmarkId, futureTime)

        assertNotNull(reminderRepository.getByBookmarkId(bookmarkId))

        // Deleting the bookmark should cascade-delete its reminder in SQLite
        bookmarkRepository.deleteById(bookmarkId)
        assertNull(reminderRepository.getByBookmarkId(bookmarkId))
    }
}
