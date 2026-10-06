package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddBookmarkUseCaseTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var repository: BookmarkRepository
    private lateinit var addBookmarkUseCase: AddBookmarkUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        repository = BookmarkRepositoryImpl(database.bookmarkDao())
        addBookmarkUseCase = AddBookmarkUseCase(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testValidUrlSavesBookmark() = runBlocking {
        val result = addBookmarkUseCase("https://example.com/page")
        assertTrue(result is AddBookmarkResult.Success)
        val success = result as AddBookmarkResult.Success
        assertEquals("https://example.com/page", success.bookmark.url)
        assertEquals("example.com", success.bookmark.domain)
        assertTrue(success.bookmark.id > 0)

        val persisted = repository.getById(success.bookmark.id)
        assertEquals(success.bookmark.id, persisted?.id)
    }

    @Test
    fun testUrlWithoutSchemeAddsHttps() = runBlocking {
        val result = addBookmarkUseCase("kotlinlang.org")
        assertTrue(result is AddBookmarkResult.Success)
        val success = result as AddBookmarkResult.Success
        assertEquals("https://kotlinlang.org", success.bookmark.url)
        assertEquals("kotlinlang.org", success.bookmark.domain)
    }

    @Test
    fun testBlankUrlReturnsInvalidUrl() = runBlocking {
        val result = addBookmarkUseCase("   ")
        assertTrue(result is AddBookmarkResult.InvalidUrl)
    }

    @Test
    fun testInvalidUrlReturnsInvalidUrl() = runBlocking {
        val result = addBookmarkUseCase("hello_world_not_url")
        assertTrue(result is AddBookmarkResult.InvalidUrl)
    }

    @Test
    fun testDuplicateUrlReturnsDuplicateResult() = runBlocking {
        val firstResult = addBookmarkUseCase("https://example.com/docs")
        assertTrue(firstResult is AddBookmarkResult.Success)

        // Attempting to save the same normalized URL
        val secondResult = addBookmarkUseCase("https://example.com/docs/")
        assertTrue(secondResult is AddBookmarkResult.Duplicate)
        val duplicate = secondResult as AddBookmarkResult.Duplicate
        assertEquals("https://example.com/docs", duplicate.existing.normalizedUrl)
    }

    @Test
    fun testDuplicateWithForceSaveSavesAnyway() = runBlocking {
        val firstResult = addBookmarkUseCase("https://example.com/tools")
        assertTrue(firstResult is AddBookmarkResult.Success)

        // Force save duplicate
        val forceResult = addBookmarkUseCase("https://example.com/tools", forceSave = true)
        assertTrue(forceResult is AddBookmarkResult.Success)

        val matches = repository.findByNormalizedUrl("https://example.com/tools")
        assertEquals(2, matches.size)
    }

    @Test
    fun testBookmarkSavesImmediatelyEvenIfMetadataExtractionFails() = runBlocking {
        val failingMetadataRepository = object : com.pixelmarks.data.repository.MetadataRepository {
            override suspend fun fetchMetadata(url: String): com.pixelmarks.core.model.MetadataFetchResult {
                throw RuntimeException("Network crash")
            }
        }
        val fetchMetadataUseCase = FetchBookmarkMetadataUseCase(repository, failingMetadataRepository)
        val useCaseWithFailingMetadata = AddBookmarkUseCase(
            bookmarkRepository = repository,
            fetchBookmarkMetadataUseCase = fetchMetadataUseCase,
            coroutineScope = this
        )

        val result = useCaseWithFailingMetadata("https://resilient.example.com")
        assertTrue(result is AddBookmarkResult.Success)
        val success = result as AddBookmarkResult.Success
        assertTrue(success.bookmark.id > 0)

        // Bookmark is guaranteed to be persisted in SQLite
        val persisted = repository.getById(success.bookmark.id)
        org.junit.Assert.assertNotNull(persisted)
        assertEquals("https://resilient.example.com", persisted?.url)
    }
}
