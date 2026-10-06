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
class BookmarkRepositoryTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var repository: BookmarkRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        repository = BookmarkRepositoryImpl(database.bookmarkDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveBookmark() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark = Bookmark(
            url = "https://example.com/article",
            normalizedUrl = "https://example.com/article",
            title = "Example Article",
            domain = "example.com",
            createdAt = now,
            updatedAt = now
        )

        val id = repository.insert(bookmark)
        assertTrue(id > 0)

        val retrieved = repository.getById(id)
        assertNotNull(retrieved)
        assertEquals("Example Article", retrieved?.title)
        assertEquals("https://example.com/article", retrieved?.normalizedUrl)
    }

    @Test
    fun testObserveAllBookmarks() = runBlocking {
        val now = System.currentTimeMillis()
        repository.insert(
            Bookmark(
                url = "https://a.com",
                normalizedUrl = "https://a.com",
                title = "A",
                domain = "a.com",
                createdAt = now,
                updatedAt = now
            )
        )
        repository.insert(
            Bookmark(
                url = "https://b.com",
                normalizedUrl = "https://b.com",
                title = "B",
                domain = "b.com",
                createdAt = now + 1000,
                updatedAt = now + 1000
            )
        )

        val list = repository.observeAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun testFindByNormalizedUrl() = runBlocking {
        val now = System.currentTimeMillis()
        repository.insert(
            Bookmark(
                url = "https://kotlinlang.org",
                normalizedUrl = "https://kotlinlang.org",
                title = "Kotlin",
                domain = "kotlinlang.org",
                createdAt = now,
                updatedAt = now
            )
        )

        val matches = repository.findByNormalizedUrl("https://kotlinlang.org")
        assertEquals(1, matches.size)
        assertEquals("Kotlin", matches.first().title)

        val noMatches = repository.findByNormalizedUrl("https://notfound.org")
        assertTrue(noMatches.isEmpty())
    }

    @Test
    fun testUpdateBookmark() = runBlocking {
        val now = System.currentTimeMillis()
        val id = repository.insert(
            Bookmark(
                url = "https://android.com",
                normalizedUrl = "https://android.com",
                title = "Android",
                domain = "android.com",
                createdAt = now,
                updatedAt = now
            )
        )

        val original = repository.getById(id)!!
        repository.update(original.copy(title = "Updated Android", isFavorite = true))

        val updated = repository.getById(id)
        assertEquals("Updated Android", updated?.title)
        assertTrue(updated?.isFavorite == true)
    }

    @Test
    fun testDeleteBookmark() = runBlocking {
        val now = System.currentTimeMillis()
        val id = repository.insert(
            Bookmark(
                url = "https://delete-me.com",
                normalizedUrl = "https://delete-me.com",
                title = "Delete Me",
                domain = "delete-me.com",
                createdAt = now,
                updatedAt = now
            )
        )

        assertNotNull(repository.getById(id))
        repository.deleteById(id)
        assertNull(repository.getById(id))
    }
}
