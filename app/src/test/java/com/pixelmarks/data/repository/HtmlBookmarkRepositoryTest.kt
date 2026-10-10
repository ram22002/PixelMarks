package com.pixelmarks.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.model.html.HtmlBookmarkItem
import com.pixelmarks.core.model.html.HtmlExportScope
import com.pixelmarks.core.model.html.HtmlImportCollectionTarget
import com.pixelmarks.core.model.html.HtmlImportDuplicatePolicy
import com.pixelmarks.core.model.html.HtmlImportOptions
import com.pixelmarks.core.model.html.HtmlParseResult
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class HtmlBookmarkRepositoryTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var repository: HtmlBookmarkRepository
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            PixelMarksDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = HtmlBookmarkRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPreviewRespectsPrivacyBoundary() = runBlocking {
        // Seed database with a private bookmark
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1L,
                url = "https://private.example.com",
                normalizedUrl = "https://private.example.com",
                title = "Private Title",
                domain = "private.example.com",
                isPrivate = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val parseResult = HtmlParseResult(
            totalFound = 1,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://private.example.com",
                    normalizedUrl = "https://private.example.com",
                    title = "Attempted Import",
                    folderPath = emptyList()
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = emptyList(),
            fileDuplicateCount = 0
        )

        // In Public mode, the private bookmark MUST NOT be recognized as a duplicate!
        val publicPreview = repository.getImportPreview(parseResult, isPrivate = false)
        assertEquals(0, publicPreview.existingDbDuplicateCount)

        // In Private mode, it SHOULD be recognized as a duplicate
        val privatePreview = repository.getImportPreview(parseResult, isPrivate = true)
        assertEquals(1, privatePreview.existingDbDuplicateCount)
    }

    @Test
    fun testImportWithSkipDuplicatesPolicy() = runBlocking {
        // Seed public bookmark
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1L,
                url = "https://existing.com",
                normalizedUrl = "https://existing.com",
                title = "Original Title",
                domain = "existing.com",
                isPrivate = false,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val parseResult = HtmlParseResult(
            totalFound = 2,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://existing.com",
                    normalizedUrl = "https://existing.com",
                    title = "New Title",
                    folderPath = emptyList()
                ),
                HtmlBookmarkItem(
                    url = "https://brandnew.com",
                    normalizedUrl = "https://brandnew.com",
                    title = "Brand New",
                    folderPath = emptyList()
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = emptyList(),
            fileDuplicateCount = 0
        )

        val summary = repository.importBookmarks(
            parseResult = parseResult,
            options = HtmlImportOptions(duplicatePolicy = HtmlImportDuplicatePolicy.SKIP),
            isPrivate = false
        )

        assertEquals(1, summary.importedCount)
        assertEquals(1, summary.skippedDuplicatesCount)
        assertEquals(0, summary.updatedCount)

        // Verify existing was NOT overwritten
        val original = database.bookmarkDao().getById(1L)
        assertNotNull(original)
        assertEquals("Original Title", original!!.title)

        // Verify brand new was inserted
        val allPublic = database.bookmarkDao().getAllByPrivacy(false)
        assertEquals(2, allPublic.size)
    }

    @Test
    fun testImportWithUpdateDuplicatesPolicy() = runBlocking {
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1L,
                url = "https://existing.com",
                normalizedUrl = "https://existing.com",
                title = "Old Title",
                domain = "existing.com",
                isPrivate = false,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val parseResult = HtmlParseResult(
            totalFound = 1,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://existing.com",
                    normalizedUrl = "https://existing.com",
                    title = "Updated Title",
                    description = "Updated description",
                    tags = listOf("updated-tag"),
                    folderPath = emptyList()
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = emptyList(),
            fileDuplicateCount = 0
        )

        val summary = repository.importBookmarks(
            parseResult = parseResult,
            options = HtmlImportOptions(duplicatePolicy = HtmlImportDuplicatePolicy.UPDATE),
            isPrivate = false
        )

        assertEquals(0, summary.importedCount)
        assertEquals(1, summary.updatedCount)
        assertEquals(0, summary.skippedDuplicatesCount)

        val updated = database.bookmarkDao().getById(1L)
        assertNotNull(updated)
        assertEquals("Updated Title", updated!!.title)
        assertEquals("Updated description", updated.description)

        val tags = database.bookmarkTagDao().getTagsForBookmark(1L)
        assertEquals(1, tags.size)
        assertEquals("updated-tag", tags[0].name)
    }

    @Test
    fun testImportNestedFolderHierarchyPreservation() = runBlocking {
        val parseResult = HtmlParseResult(
            totalFound = 2,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://android.com",
                    normalizedUrl = "https://android.com",
                    title = "Android",
                    folderPath = listOf("Tech", "Mobile", "Android")
                ),
                HtmlBookmarkItem(
                    url = "https://ios.com",
                    normalizedUrl = "https://ios.com",
                    title = "iOS",
                    folderPath = listOf("Tech", "Mobile", "iOS")
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = listOf("Tech", "Mobile", "Android", "iOS"),
            fileDuplicateCount = 0
        )

        val summary = repository.importBookmarks(
            parseResult = parseResult,
            options = HtmlImportOptions(
                collectionTarget = HtmlImportCollectionTarget.PreserveFolders
            ),
            isPrivate = false
        )

        assertEquals(2, summary.importedCount)
        assertEquals(4, summary.createdCollectionsCount)

        val allCols = database.collectionDao().getAllByPrivacy(false)
        assertEquals(4, allCols.size)

        val tech = allCols.first { it.name == "Tech" }
        assertNull(tech.parentId)

        val mobile = allCols.first { it.name == "Mobile" }
        assertEquals(tech.id, mobile.parentId)

        val androidCol = allCols.first { it.name == "Android" }
        assertEquals(mobile.id, androidCol.parentId)

        val iosCol = allCols.first { it.name == "iOS" }
        assertEquals(mobile.id, iosCol.parentId)

        val androidBookmark = database.bookmarkDao().getByNormalizedUrlAndPrivacy("https://android.com", false).first()
        assertEquals(androidCol.id, androidBookmark.collectionId)
        assertEquals(MetadataStatus.PENDING, androidBookmark.metadataStatus)
    }

    @Test
    fun testExportPublicDoesNotLeakPrivate() = runBlocking {
        // Seed public bookmark and collection
        val publicColId = database.collectionDao().insert(
            CollectionEntity(name = "Public Folder", isPrivate = false, createdAt = 1000L, updatedAt = 1000L)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://public.com",
                normalizedUrl = "https://public.com",
                title = "Public Bookmark",
                domain = "public.com",
                collectionId = publicColId,
                isPrivate = false,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Seed private bookmark and collection
        val privateColId = database.collectionDao().insert(
            CollectionEntity(name = "Secret Folder", isPrivate = true, createdAt = 1000L, updatedAt = 1000L)
        )
        database.bookmarkDao().insert(
            BookmarkEntity(
                url = "https://secret.com",
                normalizedUrl = "https://secret.com",
                title = "Secret Bookmark",
                domain = "secret.com",
                collectionId = privateColId,
                isPrivate = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val baos = ByteArrayOutputStream()
        val exportResult = repository.exportBookmarks(
            scope = HtmlExportScope.All,
            isPrivate = false,
            outputStream = baos
        )

        assertEquals(1, exportResult.exportedBookmarksCount)
        assertEquals(1, exportResult.exportedCollectionsCount)
        assertFalse(exportResult.isPrivate)

        val html = baos.toString("UTF-8")
        assertTrue(html.contains("Public Bookmark"))
        assertTrue(html.contains("Public Folder"))
        assertFalse(html.contains("Secret Bookmark"))
        assertFalse(html.contains("Secret Folder"))
        assertFalse(html.contains("https://secret.com"))
    }

    @Test
    fun testImportWithImportNewDuplicatePolicy() = runBlocking {
        database.bookmarkDao().insert(
            BookmarkEntity(
                id = 1L,
                url = "https://dup.com",
                normalizedUrl = "https://dup.com",
                title = "Original",
                domain = "dup.com",
                isPrivate = false,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val parseResult = HtmlParseResult(
            totalFound = 1,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://dup.com",
                    normalizedUrl = "https://dup.com",
                    title = "Imported Duplicate",
                    folderPath = emptyList()
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = emptyList(),
            fileDuplicateCount = 0
        )

        val summary = repository.importBookmarks(
            parseResult = parseResult,
            options = HtmlImportOptions(duplicatePolicy = HtmlImportDuplicatePolicy.IMPORT_NEW),
            isPrivate = false
        )

        assertEquals(1, summary.importedCount)
        assertEquals(0, summary.skippedDuplicatesCount)
        assertEquals(0, summary.updatedCount)

        val duplicates = database.bookmarkDao().getByNormalizedUrlAndPrivacy("https://dup.com", false)
        assertEquals(2, duplicates.size)
    }

    @Test
    fun testImportTargetRootOnlyIgnoresFolders() = runBlocking {
        val parseResult = HtmlParseResult(
            totalFound = 1,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://example.com/root",
                    normalizedUrl = "https://example.com/root",
                    title = "Root Bookmark",
                    folderPath = listOf("Deep", "Folder", "Structure")
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = listOf("Deep", "Folder", "Structure"),
            fileDuplicateCount = 0
        )

        val summary = repository.importBookmarks(
            parseResult = parseResult,
            options = HtmlImportOptions(collectionTarget = HtmlImportCollectionTarget.RootOnly),
            isPrivate = false
        )

        assertEquals(1, summary.importedCount)
        assertEquals(0, summary.createdCollectionsCount)

        val bookmark = database.bookmarkDao().getByNormalizedUrlAndPrivacy("https://example.com/root", false).first()
        assertNull(bookmark.collectionId)
    }

    @Test
    fun testImportTargetExistingCollection() = runBlocking {
        val colId = database.collectionDao().insert(
            CollectionEntity(name = "Target Collection", isPrivate = false, createdAt = 1000L, updatedAt = 1000L)
        )

        val parseResult = HtmlParseResult(
            totalFound = 1,
            validBookmarks = listOf(
                HtmlBookmarkItem(
                    url = "https://target.com",
                    normalizedUrl = "https://target.com",
                    title = "Targeted",
                    folderPath = listOf("Some", "Folder")
                )
            ),
            invalidEntries = emptyList(),
            detectedFolders = listOf("Some", "Folder"),
            fileDuplicateCount = 0
        )

        val summary = repository.importBookmarks(
            parseResult = parseResult,
            options = HtmlImportOptions(
                collectionTarget = HtmlImportCollectionTarget.ExistingCollection(colId)
            ),
            isPrivate = false
        )

        assertEquals(1, summary.importedCount)
        assertEquals(0, summary.createdCollectionsCount)

        val bookmark = database.bookmarkDao().getByNormalizedUrlAndPrivacy("https://target.com", false).first()
        assertEquals(colId, bookmark.collectionId)
    }
}

