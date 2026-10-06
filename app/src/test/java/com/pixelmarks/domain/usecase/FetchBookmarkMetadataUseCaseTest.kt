package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.core.model.PageMetadata
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.data.repository.MetadataRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FetchBookmarkMetadataUseCaseTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var testMetadataRepository: FakeMetadataRepository
    private lateinit var useCase: FetchBookmarkMetadataUseCase

    class FakeMetadataRepository : MetadataRepository {
        var resultToReturn: MetadataFetchResult = MetadataFetchResult.NoMetadata

        override suspend fun fetchMetadata(url: String): MetadataFetchResult {
            return resultToReturn
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())
        testMetadataRepository = FakeMetadataRepository()
        useCase = FetchBookmarkMetadataUseCase(bookmarkRepository, testMetadataRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun successfulMetadataUpdate_replacesFallbackTitleAndEnrichesFields() = runBlocking {
        val bookmarkId = bookmarkRepository.insert(
            Bookmark(
                url = "https://example.com/blog/1",
                normalizedUrl = "https://example.com/blog/1",
                title = "example.com", // fallback title
                domain = "example.com"
            )
        )

        testMetadataRepository.resultToReturn = MetadataFetchResult.Success(
            PageMetadata(
                title = "Awesome Article Title",
                description = "Deep dive into Kotlin and Compose.",
                imageUrl = "https://example.com/img/card.png",
                faviconUrl = "https://example.com/favicon.ico"
            )
        )

        val status = useCase(bookmarkId)
        assertEquals(MetadataStatus.SUCCESS, status)

        val updated = bookmarkRepository.getById(bookmarkId)
        assertNotNull(updated)
        assertEquals("Awesome Article Title", updated?.title)
        assertEquals("Deep dive into Kotlin and Compose.", updated?.description)
        assertEquals("https://example.com/img/card.png", updated?.thumbnailUrl)
        assertEquals("https://example.com/favicon.ico", updated?.faviconUrl)
        assertEquals(MetadataStatus.SUCCESS, updated?.metadataStatus)
        assertNotNull(updated?.metadataUpdatedAt)
    }

    @Test
    fun successfulMetadataUpdate_preservesCustomUserTitle() = runBlocking {
        val bookmarkId = bookmarkRepository.insert(
            Bookmark(
                url = "https://example.com/blog/1",
                normalizedUrl = "https://example.com/blog/1",
                title = "My Custom Renamed Bookmark", // user-supplied custom title
                domain = "example.com"
            )
        )

        testMetadataRepository.resultToReturn = MetadataFetchResult.Success(
            PageMetadata(
                title = "Webpage Scraped Title",
                description = "Some description."
            )
        )

        val status = useCase(bookmarkId)
        assertEquals(MetadataStatus.SUCCESS, status)

        val updated = bookmarkRepository.getById(bookmarkId)
        assertNotNull(updated)
        // User's custom title should not be overwritten!
        assertEquals("My Custom Renamed Bookmark", updated?.title)
        assertEquals("Some description.", updated?.description)
        assertEquals(MetadataStatus.SUCCESS, updated?.metadataStatus)
    }

    @Test
    fun metadataFailure_preservesBookmarkAndSetsStatusFailed() = runBlocking {
        val bookmarkId = bookmarkRepository.insert(
            Bookmark(
                url = "https://broken.example.com",
                normalizedUrl = "https://broken.example.com",
                title = "broken.example.com",
                domain = "broken.example.com"
            )
        )

        testMetadataRepository.resultToReturn = MetadataFetchResult.Failed("Connection timed out")

        val status = useCase(bookmarkId)
        assertEquals(MetadataStatus.FAILED, status)

        val preserved = bookmarkRepository.getById(bookmarkId)
        assertNotNull(preserved)
        assertEquals("broken.example.com", preserved?.title)
        assertEquals(MetadataStatus.FAILED, preserved?.metadataStatus)
    }

    @Test
    fun http403_doesNotDeleteBookmark() = runBlocking {
        val bookmarkId = bookmarkRepository.insert(
            Bookmark(
                url = "https://secret.example.com",
                normalizedUrl = "https://secret.example.com",
                title = "secret.example.com",
                domain = "secret.example.com"
            )
        )

        testMetadataRepository.resultToReturn = MetadataFetchResult.Failed("HTTP 403", statusCode = 403)

        val status = useCase(bookmarkId)
        assertEquals(MetadataStatus.FAILED, status)

        val preserved = bookmarkRepository.getById(bookmarkId)
        assertNotNull(preserved)
        assertEquals(MetadataStatus.FAILED, preserved?.metadataStatus)
    }

    @Test
    fun missingMetadata_producesNoMetadataStatus() = runBlocking {
        val bookmarkId = bookmarkRepository.insert(
            Bookmark(
                url = "https://empty.example.com",
                normalizedUrl = "https://empty.example.com",
                title = "empty.example.com",
                domain = "empty.example.com"
            )
        )

        testMetadataRepository.resultToReturn = MetadataFetchResult.NoMetadata

        val status = useCase(bookmarkId)
        assertEquals(MetadataStatus.NO_METADATA, status)

        val preserved = bookmarkRepository.getById(bookmarkId)
        assertNotNull(preserved)
        assertEquals(MetadataStatus.NO_METADATA, preserved?.metadataStatus)
    }

    @Test
    fun blockedMetadata_producesBlockedStatus() = runBlocking {
        val bookmarkId = bookmarkRepository.insert(
            Bookmark(
                url = "https://large.example.com/video.mp4",
                normalizedUrl = "https://large.example.com/video.mp4",
                title = "large.example.com",
                domain = "large.example.com"
            )
        )

        testMetadataRepository.resultToReturn = MetadataFetchResult.Blocked("Response exceeded size limit")

        val status = useCase(bookmarkId)
        assertEquals(MetadataStatus.BLOCKED, status)

        val preserved = bookmarkRepository.getById(bookmarkId)
        assertNotNull(preserved)
        assertEquals(MetadataStatus.BLOCKED, preserved?.metadataStatus)
    }
}
