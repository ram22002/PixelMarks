package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.core.model.PageMetadata
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.data.repository.MetadataRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditBookmarkFlowTest {

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
    fun testEditBookmarkDetails_preservesIdentityAndLifecycle() = runBlocking {
        val now = System.currentTimeMillis()
        val original = Bookmark(
            url = "https://example.com/item",
            normalizedUrl = "https://example.com/item",
            title = "Original Title",
            description = "Original Description",
            domain = "example.com",
            notes = "Original Note",
            isFavorite = true,
            isPinned = true,
            createdAt = now,
            updatedAt = now
        )
        val id = repository.insert(original)

        val retrieved = repository.getById(id)
        assertNotNull(retrieved)

        val updated = retrieved!!.copy(
            title = "User Edited Title",
            description = "User Edited Description",
            notes = "Updated Note",
            thumbnailUrl = "https://example.com/new_image.jpg",
            updatedAt = System.currentTimeMillis()
        )
        repository.update(updated)

        val saved = repository.getById(id)
        assertNotNull(saved)
        assertEquals("User Edited Title", saved?.title)
        assertEquals("User Edited Description", saved?.description)
        assertEquals("Updated Note", saved?.notes)
        assertEquals("https://example.com/new_image.jpg", saved?.thumbnailUrl)
        assertTrue(saved?.isFavorite == true)
        assertTrue(saved?.isPinned == true)
    }

    @Test
    fun testUrlChange_updatesNormalizedUrlAndDomain() = runBlocking {
        val now = System.currentTimeMillis()
        val original = Bookmark(
            url = "https://old.com",
            normalizedUrl = "https://old.com",
            title = "Old",
            domain = "old.com",
            createdAt = now,
            updatedAt = now
        )
        val id = repository.insert(original)

        val newUrl = "https://new-domain.com/path/"
        val normalized = UrlUtils.normalizeUrl(newUrl)
        val newDomain = UrlUtils.extractDomain(normalized)

        val updated = repository.getById(id)!!.copy(
            url = normalized,
            normalizedUrl = normalized,
            domain = newDomain,
            updatedAt = System.currentTimeMillis()
        )
        repository.update(updated)

        val saved = repository.getById(id)
        assertEquals("https://new-domain.com/path", saved?.url)
        assertEquals("https://new-domain.com/path", saved?.normalizedUrl)
        assertEquals("new-domain.com", saved?.domain)
    }

    @Test
    fun testUserManualEdits_notOverwrittenByMetadataRefresh() = runBlocking {
        val now = System.currentTimeMillis()
        val bookmark = Bookmark(
            url = "https://article.com",
            normalizedUrl = "https://article.com",
            title = "User Custom Title",
            description = "User Custom Description",
            domain = "article.com",
            thumbnailUrl = "https://article.com/user_image.png",
            createdAt = now,
            updatedAt = now
        )
        val id = repository.insert(bookmark)

        // Mock metadata repository returning different title, description, and image
        val fakeMetadataRepo = object : MetadataRepository {
            override suspend fun fetchMetadata(url: String): MetadataFetchResult {
                return MetadataFetchResult.Success(
                    PageMetadata(
                        title = "Overriding Remote Title",
                        description = "Overriding Remote Description",
                        imageUrl = "https://article.com/remote_image.png",
                        faviconUrl = "https://article.com/favicon.ico"
                    )
                )
            }
        }

        val fetchMetadataUseCase = FetchBookmarkMetadataUseCase(repository, fakeMetadataRepo)
        fetchMetadataUseCase(id)

        val afterEnrichment = repository.getById(id)
        assertNotNull(afterEnrichment)
        // User custom title, description, and manual image must be preserved!
        assertEquals("User Custom Title", afterEnrichment?.title)
        assertEquals("User Custom Description", afterEnrichment?.description)
        assertEquals("https://article.com/user_image.png", afterEnrichment?.thumbnailUrl)
        // Favicon can be enriched
        assertEquals("https://article.com/favicon.ico", afterEnrichment?.faviconUrl)
    }
}
