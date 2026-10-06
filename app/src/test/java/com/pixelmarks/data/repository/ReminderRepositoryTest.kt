package com.pixelmarks.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.reminder.ReminderFormatter
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
import java.util.Calendar

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
        reminderRepository = ReminderRepositoryImpl(database.reminderDao(), bookmarkRepository, context)
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
    fun testNoReminderState() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val reminder = reminderRepository.getByBookmarkId(bookmarkId)
        assertNull(reminder)

        val observed = reminderRepository.observeByBookmarkId(bookmarkId).first()
        assertNull(observed)
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
    fun testDuplicatePrevention_updatesExistingRow() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val initialTime = System.currentTimeMillis() + 3600000L
        val updatedTime = System.currentTimeMillis() + 7200000L

        val firstReminderId = reminderRepository.setReminder(bookmarkId, initialTime)
        val secondReminderId = reminderRepository.setReminder(bookmarkId, updatedTime)

        // Must update the existing reminder, not create a duplicate row
        assertEquals(firstReminderId, secondReminderId)

        val allReminders = reminderRepository.observeAll().first()
        assertEquals(1, allReminders.size)
        assertEquals(updatedTime, allReminders.first().reminderAt)
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
    fun testReactiveReminderUpdates() = runBlocking {
        val bookmarkId = insertTestBookmark()
        assertNull(reminderRepository.observeByBookmarkId(bookmarkId).first())

        val time = System.currentTimeMillis() + 3600000L
        reminderRepository.setReminder(bookmarkId, time)
        val afterSet = reminderRepository.observeByBookmarkId(bookmarkId).first()
        assertNotNull(afterSet)
        assertEquals(time, afterSet?.reminderAt)

        reminderRepository.cancelReminder(bookmarkId)
        assertNull(reminderRepository.observeByBookmarkId(bookmarkId).first())
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
    fun testReminderSurvivesNormalBookmarkEdit() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 3600000L
        reminderRepository.setReminder(bookmarkId, futureTime)

        val bookmark = bookmarkRepository.getById(bookmarkId)!!
        bookmarkRepository.update(bookmark.copy(title = "Edited Title", notes = "New note"))

        val reminder = reminderRepository.getByBookmarkId(bookmarkId)
        assertNotNull(reminder)
        assertEquals(futureTime, reminder?.reminderAt)
    }

    @Test
    fun testReminderSurvivesMovingToTrashAndRestore() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 3600000L
        reminderRepository.setReminder(bookmarkId, futureTime)

        // Move to trash
        val bookmark = bookmarkRepository.getById(bookmarkId)!!
        bookmarkRepository.update(bookmark.copy(isTrashed = true, trashedAt = System.currentTimeMillis()))
        assertNotNull(reminderRepository.getByBookmarkId(bookmarkId))

        // Restore from trash
        val trashedBookmark = bookmarkRepository.getById(bookmarkId)!!
        bookmarkRepository.update(trashedBookmark.copy(isTrashed = false, trashedAt = null))
        val reminder = reminderRepository.getByBookmarkId(bookmarkId)
        assertNotNull(reminder)
        assertEquals(futureTime, reminder?.reminderAt)
    }

    @Test
    fun testCascadeDeleteWithBookmark() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val futureTime = System.currentTimeMillis() + 3600000L
        reminderRepository.setReminder(bookmarkId, futureTime)

        assertNotNull(reminderRepository.getByBookmarkId(bookmarkId))

        // Permanently deleting the bookmark cascades in Room/SQLite
        bookmarkRepository.deleteById(bookmarkId)
        assertNull(reminderRepository.getByBookmarkId(bookmarkId))
    }

    @Test
    fun testReminderFormatter_todayAndTomorrow() {
        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        val now = nowCal.timeInMillis

        val todayTarget = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 20) // 8:00 PM
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val formattedToday = ReminderFormatter.format(todayTarget, now)
        assertTrue(formattedToday.startsWith("Today ·"))

        val tomorrowTarget = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 20) // 8:00 PM
            set(Calendar.MINUTE, 0)
        }.timeInMillis

        val formattedTomorrow = ReminderFormatter.format(tomorrowTarget, now)
        assertTrue(formattedTomorrow.startsWith("Tomorrow ·"))
    }

    @Test
    fun testCustomReminderPersistedCorrectly() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val customFutureTime = System.currentTimeMillis() + (5 * 24 * 3600 * 1000L) // 5 days later

        val reminderId = reminderRepository.setReminder(bookmarkId, customFutureTime)
        assertTrue(reminderId > 0)

        val retrieved = reminderRepository.getByBookmarkId(bookmarkId)
        assertNotNull(retrieved)
        assertEquals(customFutureTime, retrieved?.reminderAt)

        val bookmark = bookmarkRepository.getById(bookmarkId)
        assertEquals(customFutureTime, bookmark?.reminderAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCustomReminder_pastTimeRejected() {
        runBlocking {
            val bookmarkId = insertTestBookmark()
            val pastTime = System.currentTimeMillis() - 60000L // 1 minute in the past
            reminderRepository.setReminder(bookmarkId, pastTime)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCustomReminder_negativeTimeRejected() {
        runBlocking {
            val bookmarkId = insertTestBookmark()
            reminderRepository.setReminder(bookmarkId, -5000L)
        }
    }

    @Test
    fun testExistingReminderUpdatedWithCustomTime() = runBlocking {
        val bookmarkId = insertTestBookmark()
        val initialPresetTime = System.currentTimeMillis() + 3600000L
        val customTime = System.currentTimeMillis() + (10 * 24 * 3600 * 1000L)

        val id1 = reminderRepository.setReminder(bookmarkId, initialPresetTime)
        val id2 = reminderRepository.setReminder(bookmarkId, customTime)

        // Must update existing reminder id and timestamp without duplicates
        assertEquals(id1, id2)

        val retrieved = reminderRepository.getByBookmarkId(bookmarkId)
        assertEquals(customTime, retrieved?.reminderAt)

        val all = reminderRepository.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(customTime, all.first().reminderAt)
    }
}

