package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecordBookmarkVisitUseCaseTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var recordBookmarkVisitUseCase: RecordBookmarkVisitUseCase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = PixelMarksDatabase.buildInMemory(context)
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())
        recordBookmarkVisitUseCase = RecordBookmarkVisitUseCase(bookmarkRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordVisit_incrementsCountAndUpdatesLastOpenedAtWithoutAlteringMetadata() = runBlocking {
        val initialBookmark = Bookmark(
            url = "https://kotlinlang.org",
            normalizedUrl = "https://kotlinlang.org",
            title = "Kotlin Programming",
            description = "Modern concise language",
            domain = "kotlinlang.org",
            faviconUrl = "https://kotlinlang.org/favicon.ico",
            thumbnailUrl = "https://kotlinlang.org/preview.png",
            collectionId = null,
            isFavorite = true,
            isPinned = true,
            isArchived = false,
            isTrashed = false,
            notes = "Very helpful notes",
            reminderAt = null,
            metadataStatus = MetadataStatus.SUCCESS,
            visitCount = 0,
            lastOpenedAt = null
        )

        val id = bookmarkRepository.insert(initialBookmark)

        // First visit
        val firstOpenedAt = 1000L
        recordBookmarkVisitUseCase(id, firstOpenedAt)

        val afterFirstVisit = bookmarkRepository.getById(id)
        assertNotNull(afterFirstVisit)
        assertEquals(1, afterFirstVisit!!.visitCount)
        assertEquals(firstOpenedAt, afterFirstVisit.lastOpenedAt)

        // Verify other metadata is unchanged
        assertEquals("Kotlin Programming", afterFirstVisit.title)
        assertEquals("Modern concise language", afterFirstVisit.description)
        assertEquals("kotlinlang.org", afterFirstVisit.domain)
        assertEquals("https://kotlinlang.org/favicon.ico", afterFirstVisit.faviconUrl)
        assertEquals("https://kotlinlang.org/preview.png", afterFirstVisit.thumbnailUrl)
        assertEquals(true, afterFirstVisit.isFavorite)
        assertEquals(true, afterFirstVisit.isPinned)
        assertEquals(false, afterFirstVisit.isArchived)
        assertEquals(false, afterFirstVisit.isTrashed)
        assertEquals("Very helpful notes", afterFirstVisit.notes)

        // Second visit
        val secondOpenedAt = 2500L
        recordBookmarkVisitUseCase(id, secondOpenedAt)

        val afterSecondVisit = bookmarkRepository.getById(id)
        assertNotNull(afterSecondVisit)
        assertEquals(2, afterSecondVisit!!.visitCount)
        assertEquals(secondOpenedAt, afterSecondVisit.lastOpenedAt)

        // Verify metadata still unchanged
        assertEquals("Kotlin Programming", afterSecondVisit.title)
        assertEquals("Modern concise language", afterSecondVisit.description)
        assertEquals(true, afterSecondVisit.isFavorite)
    }
}
