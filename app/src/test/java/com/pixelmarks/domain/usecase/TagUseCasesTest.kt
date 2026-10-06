package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.data.repository.CollectionRepositoryImpl
import com.pixelmarks.data.repository.TagRepository
import com.pixelmarks.data.repository.TagRepositoryImpl
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
class TagUseCasesTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var tagRepository: TagRepository
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var collectionRepository: CollectionRepository

    private lateinit var observeTagsUseCase: ObserveTagsUseCase
    private lateinit var getTagUseCase: GetTagUseCase
    private lateinit var createTagUseCase: CreateTagUseCase
    private lateinit var updateTagUseCase: UpdateTagUseCase
    private lateinit var deleteTagUseCase: DeleteTagUseCase
    private lateinit var observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase
    private lateinit var observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase
    private lateinit var replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase
    private lateinit var addBookmarkUseCase: AddBookmarkUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        tagRepository = TagRepositoryImpl(database.tagDao(), database.bookmarkTagDao())
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())
        collectionRepository = CollectionRepositoryImpl(database.collectionDao())

        observeTagsUseCase = ObserveTagsUseCase(tagRepository)
        getTagUseCase = GetTagUseCase(tagRepository)
        createTagUseCase = CreateTagUseCase(tagRepository)
        updateTagUseCase = UpdateTagUseCase(tagRepository)
        deleteTagUseCase = DeleteTagUseCase(tagRepository)
        observeBookmarkTagsUseCase = ObserveBookmarkTagsUseCase(tagRepository)
        observeBookmarksByTagUseCase = ObserveBookmarksByTagUseCase(tagRepository)
        replaceBookmarkTagsUseCase = ReplaceBookmarkTagsUseCase(tagRepository)
        addBookmarkUseCase = AddBookmarkUseCase(
            bookmarkRepository = bookmarkRepository,
            tagRepository = tagRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- TAG CREATION TESTS ---

    @Test
    fun testValidTagCreation() = runBlocking {
        val result = createTagUseCase("Android")
        assertTrue(result is CreateTagResult.Success)
        val tag = (result as CreateTagResult.Success).tag
        assertEquals("Android", tag.name)
        assertTrue(tag.id > 0)

        val retrieved = getTagUseCase(tag.id)
        assertNotNull(retrieved)
        assertEquals("Android", retrieved?.name)
    }

    @Test
    fun testWhitespaceTrimming() = runBlocking {
        val result = createTagUseCase("   Kotlin Dev   ")
        assertTrue(result is CreateTagResult.Success)
        val tag = (result as CreateTagResult.Success).tag
        assertEquals("Kotlin Dev", tag.name)
    }

    @Test
    fun testBlankNameRejected() = runBlocking {
        val blankResult = createTagUseCase("   ")
        assertTrue(blankResult is CreateTagResult.InvalidName)

        val emptyResult = createTagUseCase("")
        assertTrue(emptyResult is CreateTagResult.InvalidName)
    }

    @Test
    fun testDuplicateTagRejectedCaseInsensitive() = runBlocking {
        val first = createTagUseCase("Android")
        assertTrue(first is CreateTagResult.Success)

        val duplicate = createTagUseCase("android")
        assertTrue(duplicate is CreateTagResult.DuplicateName)

        val duplicateUpper = createTagUseCase("ANDROID")
        assertTrue(duplicateUpper is CreateTagResult.DuplicateName)
    }

    // --- TAG UPDATE TESTS ---

    @Test
    fun testValidTagRename() = runBlocking {
        val tag = (createTagUseCase("Mobile") as CreateTagResult.Success).tag
        val result = updateTagUseCase(tag.id, "Mobile Dev")
        assertTrue(result is UpdateTagResult.Success)

        val updated = getTagUseCase(tag.id)
        assertEquals("Mobile Dev", updated?.name)
    }

    @Test
    fun testRenameWhitespaceTrimmed() = runBlocking {
        val tag = (createTagUseCase("Design") as CreateTagResult.Success).tag
        val result = updateTagUseCase(tag.id, "   UI/UX Design   ")
        assertTrue(result is UpdateTagResult.Success)
        assertEquals("UI/UX Design", (result as UpdateTagResult.Success).tag.name)
    }

    @Test
    fun testRenameBlankRejected() = runBlocking {
        val tag = (createTagUseCase("Tools") as CreateTagResult.Success).tag
        val result = updateTagUseCase(tag.id, "   ")
        assertTrue(result is UpdateTagResult.InvalidName)
    }

    @Test
    fun testRenameDuplicateRejected() = runBlocking {
        createTagUseCase("News")
        val tag2 = (createTagUseCase("Blogs") as CreateTagResult.Success).tag

        val collision = updateTagUseCase(tag2.id, "news")
        assertTrue(collision is UpdateTagResult.DuplicateName)
    }

    @Test
    fun testRenameSameNameAllowed() = runBlocking {
        val tag = (createTagUseCase("Tutorials") as CreateTagResult.Success).tag
        val result = updateTagUseCase(tag.id, "Tutorials")
        assertTrue(result is UpdateTagResult.Success)
    }

    @Test
    fun testRenamePreservesBookmarkRelationships() = runBlocking {
        val tag = (createTagUseCase("Code") as CreateTagResult.Success).tag
        val bookmark = (addBookmarkUseCase("https://github.com", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark

        val renameResult = updateTagUseCase(tag.id, "Open Source")
        assertTrue(renameResult is UpdateTagResult.Success)

        val tagsForBookmark = observeBookmarkTagsUseCase(bookmark.id).first()
        assertEquals(1, tagsForBookmark.size)
        assertEquals("Open Source", tagsForBookmark[0].name)
        assertEquals(tag.id, tagsForBookmark[0].id)
    }

    // --- TAG DELETION SAFETY TESTS ---

    @Test
    fun testDeleteTagRemovesTagAndCrossRefsButPreservesBookmarks() = runBlocking {
        val tag = (createTagUseCase("TemporaryTag") as CreateTagResult.Success).tag
        val bookmark1 = (addBookmarkUseCase("https://alpha.com", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark
        val bookmark2 = (addBookmarkUseCase("https://beta.com", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark

        val deleteSuccess = deleteTagUseCase(tag.id)
        assertTrue(deleteSuccess)

        // Tag is deleted
        assertNull(getTagUseCase(tag.id))

        // Bookmarks still exist safely
        val preserved1 = bookmarkRepository.getById(bookmark1.id)
        val preserved2 = bookmarkRepository.getById(bookmark2.id)
        assertNotNull(preserved1)
        assertNotNull(preserved2)

        // Relationship was removed
        assertEquals(0, observeBookmarkTagsUseCase(bookmark1.id).first().size)
        assertEquals(0, observeBookmarkTagsUseCase(bookmark2.id).first().size)
    }

    // --- ASSIGNMENT & REPLACEMENT TESTS ---

    @Test
    fun testAssignOneTag() = runBlocking {
        val tag = (createTagUseCase("Articles") as CreateTagResult.Success).tag
        val bookmark = (addBookmarkUseCase("https://medium.com") as AddBookmarkResult.Success).bookmark

        val success = replaceBookmarkTagsUseCase(bookmark.id, setOf(tag.id))
        assertTrue(success)

        val tags = observeBookmarkTagsUseCase(bookmark.id).first()
        assertEquals(1, tags.size)
        assertEquals("Articles", tags[0].name)
    }

    @Test
    fun testAssignMultipleTags() = runBlocking {
        val tag1 = (createTagUseCase("Tech") as CreateTagResult.Success).tag
        val tag2 = (createTagUseCase("Dev") as CreateTagResult.Success).tag
        val bookmark = (addBookmarkUseCase("https://stackoverflow.com", tagIds = setOf(tag1.id, tag2.id)) as AddBookmarkResult.Success).bookmark

        val tags = observeBookmarkTagsUseCase(bookmark.id).first()
        assertEquals(2, tags.size)
        val names = tags.map { it.name }.toSet()
        assertTrue(names.contains("Tech"))
        assertTrue(names.contains("Dev"))
    }

    @Test
    fun testAssignZeroTags() = runBlocking {
        val bookmark = (addBookmarkUseCase("https://google.com") as AddBookmarkResult.Success).bookmark
        val tags = observeBookmarkTagsUseCase(bookmark.id).first()
        assertEquals(0, tags.size)
    }

    @Test
    fun testReplaceExistingTags() = runBlocking {
        val tag1 = (createTagUseCase("OldTag") as CreateTagResult.Success).tag
        val tag2 = (createTagUseCase("NewTag") as CreateTagResult.Success).tag
        val bookmark = (addBookmarkUseCase("https://wikipedia.org", tagIds = setOf(tag1.id)) as AddBookmarkResult.Success).bookmark

        // Replace tag1 with tag2
        val replaceSuccess = replaceBookmarkTagsUseCase(bookmark.id, setOf(tag2.id))
        assertTrue(replaceSuccess)

        val tags = observeBookmarkTagsUseCase(bookmark.id).first()
        assertEquals(1, tags.size)
        assertEquals("NewTag", tags[0].name)
    }

    @Test
    fun testRemoveAllTags() = runBlocking {
        val tag = (createTagUseCase("RemoveMe") as CreateTagResult.Success).tag
        val bookmark = (addBookmarkUseCase("https://example.org", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark

        val clearSuccess = replaceBookmarkTagsUseCase(bookmark.id, emptySet())
        assertTrue(clearSuccess)

        val tags = observeBookmarkTagsUseCase(bookmark.id).first()
        assertEquals(0, tags.size)
    }

    // --- REACTIVE BEHAVIOR TESTS ---

    @Test
    fun testTagCountUpdatesReactively() = runBlocking {
        val tag = (createTagUseCase("Science") as CreateTagResult.Success).tag

        val countsBefore = observeTagsUseCase.observeTagCounts().first()
        assertEquals(0, countsBefore[tag.id] ?: 0)

        // Tag one bookmark
        val bm1 = (addBookmarkUseCase("https://nature.com", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark
        val counts1 = observeTagsUseCase.observeTagCounts().first()
        assertEquals(1, counts1[tag.id])

        // Tag second bookmark
        val bm2 = (addBookmarkUseCase("https://sciencemag.org", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark
        val counts2 = observeTagsUseCase.observeTagCounts().first()
        assertEquals(2, counts2[tag.id])

        // Remove tag from bm1
        replaceBookmarkTagsUseCase(bm1.id, emptySet())
        val countsAfterRemoval = observeTagsUseCase.observeTagCounts().first()
        assertEquals(1, countsAfterRemoval[tag.id])
    }

    @Test
    fun testObserveBookmarksForTagUpdatesReactively() = runBlocking {
        val tag = (createTagUseCase("Physics") as CreateTagResult.Success).tag
        val bm = (addBookmarkUseCase("https://physics.org", tagIds = setOf(tag.id)) as AddBookmarkResult.Success).bookmark

        val list = observeBookmarksByTagUseCase(tag.id).first()
        assertEquals(1, list.size)
        assertEquals(bm.id, list[0].id)
    }

    // --- INTEGRATION TESTS ---

    @Test
    fun testTaggingDoesNotAlterBookmarkFields() = runBlocking {
        val tag = (createTagUseCase("Integration") as CreateTagResult.Success).tag
        val original = (addBookmarkUseCase(
            rawUrl = "https://kotlinlang.org",
            customTitle = "Kotlin Language"
        ) as AddBookmarkResult.Success).bookmark

        // Tag the bookmark
        replaceBookmarkTagsUseCase(original.id, setOf(tag.id))

        val retrieved = bookmarkRepository.getById(original.id)
        assertNotNull(retrieved)
        assertEquals(original.url, retrieved?.url)
        assertEquals(original.title, retrieved?.title)
        assertEquals(original.domain, retrieved?.domain)
        assertEquals(original.collectionId, retrieved?.collectionId)
        assertEquals(original.isFavorite, retrieved?.isFavorite)
        assertEquals(original.isPinned, retrieved?.isPinned)
        assertEquals(original.createdAt, retrieved?.createdAt)
        assertEquals(original.visitCount, retrieved?.visitCount)
    }
}
