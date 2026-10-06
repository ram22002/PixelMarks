package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.data.repository.CollectionRepositoryImpl
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
class CollectionUseCasesTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var bookmarkRepository: BookmarkRepository

    private lateinit var observeCollectionsUseCase: ObserveCollectionsUseCase
    private lateinit var getCollectionUseCase: GetCollectionUseCase
    private lateinit var createCollectionUseCase: CreateCollectionUseCase
    private lateinit var updateCollectionUseCase: UpdateCollectionUseCase
    private lateinit var deleteCollectionUseCase: DeleteCollectionUseCase
    private lateinit var moveBookmarkUseCase: MoveBookmarkUseCase
    private lateinit var addBookmarkUseCase: AddBookmarkUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        collectionRepository = CollectionRepositoryImpl(database.collectionDao())
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())

        observeCollectionsUseCase = ObserveCollectionsUseCase(collectionRepository)
        getCollectionUseCase = GetCollectionUseCase(collectionRepository)
        createCollectionUseCase = CreateCollectionUseCase(collectionRepository)
        updateCollectionUseCase = UpdateCollectionUseCase(collectionRepository)
        deleteCollectionUseCase = DeleteCollectionUseCase(collectionRepository, bookmarkRepository)
        moveBookmarkUseCase = MoveBookmarkUseCase(bookmarkRepository)
        addBookmarkUseCase = AddBookmarkUseCase(bookmarkRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- COLLECTION CREATION TESTS ---

    @Test
    fun testValidCollectionCreation() = runBlocking {
        val result = createCollectionUseCase(name = "Work", icon = "work", color = "#3B82F6")
        assertTrue(result is CreateCollectionResult.Success)
        val success = result as CreateCollectionResult.Success
        assertEquals("Work", success.collection.name)
        assertEquals("work", success.collection.icon)
        assertEquals("#3B82F6", success.collection.color)
        assertNull(success.collection.parentId)
        assertTrue(success.collection.id > 0)

        val retrieved = getCollectionUseCase(success.collection.id)
        assertNotNull(retrieved)
        assertEquals("Work", retrieved?.name)
    }

    @Test
    fun testBlankNameRejected() = runBlocking {
        val result = createCollectionUseCase(name = "   ")
        assertTrue(result is CreateCollectionResult.InvalidName)
    }

    @Test
    fun testWhitespaceTrimmed() = runBlocking {
        val result = createCollectionUseCase(name = "  Reading List  ")
        assertTrue(result is CreateCollectionResult.Success)
        val success = result as CreateCollectionResult.Success
        assertEquals("Reading List", success.collection.name)
    }

    @Test
    fun testDuplicateSiblingNameRejectedCaseInsensitive() = runBlocking {
        val first = createCollectionUseCase(name = "Projects")
        assertTrue(first is CreateCollectionResult.Success)

        val duplicate = createCollectionUseCase(name = "projects")
        assertTrue(duplicate is CreateCollectionResult.DuplicateName)
    }

    @Test
    fun testIdenticalNameAllowedUnderDifferentParents() = runBlocking {
        val parent1 = (createCollectionUseCase(name = "Work") as CreateCollectionResult.Success).collection
        val parent2 = (createCollectionUseCase(name = "Personal") as CreateCollectionResult.Success).collection

        val child1 = createCollectionUseCase(name = "Tasks", parentId = parent1.id)
        assertTrue(child1 is CreateCollectionResult.Success)

        val child2 = createCollectionUseCase(name = "Tasks", parentId = parent2.id)
        assertTrue(child2 is CreateCollectionResult.Success)
    }

    @Test
    fun testNestedCollectionCreation() = runBlocking {
        val parent = (createCollectionUseCase(name = "Work") as CreateCollectionResult.Success).collection
        val child = createCollectionUseCase(name = "Android", parentId = parent.id)
        assertTrue(child is CreateCollectionResult.Success)
        val childCol = (child as CreateCollectionResult.Success).collection
        assertEquals(parent.id, childCol.parentId)

        val children = observeCollectionsUseCase.observeChildren(parent.id).first()
        assertEquals(1, children.size)
        assertEquals("Android", children[0].name)
    }

    // --- HIERARCHY & UPDATE TESTS ---

    @Test
    fun testInvalidSelfParentRejected() = runBlocking {
        val col = (createCollectionUseCase(name = "Work") as CreateCollectionResult.Success).collection
        val updateResult = updateCollectionUseCase(
            collectionId = col.id,
            newName = "Work Renamed",
            newParentId = col.id
        )
        assertTrue(updateResult is UpdateCollectionResult.InvalidHierarchy)
    }

    @Test
    fun testDescendantCycleRejected() = runBlocking {
        // Hierarchy: Work (A) -> Projects (B) -> Mobile (C)
        val colA = (createCollectionUseCase(name = "Work") as CreateCollectionResult.Success).collection
        val colB = (createCollectionUseCase(name = "Projects", parentId = colA.id) as CreateCollectionResult.Success).collection
        val colC = (createCollectionUseCase(name = "Mobile", parentId = colB.id) as CreateCollectionResult.Success).collection

        // Attempting to make Work (A) a child of its descendant Mobile (C)
        val cycleResult = updateCollectionUseCase(
            collectionId = colA.id,
            newName = "Work",
            newParentId = colC.id
        )
        assertTrue(cycleResult is UpdateCollectionResult.InvalidHierarchy)
    }

    @Test
    fun testValidParentReassignment() = runBlocking {
        val work = (createCollectionUseCase(name = "Work") as CreateCollectionResult.Success).collection
        val personal = (createCollectionUseCase(name = "Personal") as CreateCollectionResult.Success).collection
        val taskItem = (createCollectionUseCase(name = "Notes", parentId = work.id) as CreateCollectionResult.Success).collection

        val updateResult = updateCollectionUseCase(
            collectionId = taskItem.id,
            newName = "Notes",
            newParentId = personal.id
        )
        assertTrue(updateResult is UpdateCollectionResult.Success)

        val updatedNotes = getCollectionUseCase(taskItem.id)
        assertEquals(personal.id, updatedNotes?.parentId)
    }

    // --- BOOKMARK ASSIGNMENT & MOVE TESTS ---

    @Test
    fun testBookmarkAssignedToCollectionOnCreation() = runBlocking {
        val col = (createCollectionUseCase(name = "Dev") as CreateCollectionResult.Success).collection
        val addResult = addBookmarkUseCase(
            rawUrl = "https://developer.android.com",
            collectionId = col.id
        )
        assertTrue(addResult is AddBookmarkResult.Success)
        val bookmark = (addResult as AddBookmarkResult.Success).bookmark
        assertEquals(col.id, bookmark.collectionId)

        val bookmarksInCol = bookmarkRepository.observeByCollection(col.id).first()
        assertEquals(1, bookmarksInCol.size)
        assertEquals(bookmark.id, bookmarksInCol[0].id)
    }

    @Test
    fun testBookmarkMovedToAnotherCollection() = runBlocking {
        val colA = (createCollectionUseCase(name = "ColA") as CreateCollectionResult.Success).collection
        val colB = (createCollectionUseCase(name = "ColB") as CreateCollectionResult.Success).collection

        val addResult = addBookmarkUseCase(
            rawUrl = "https://example.com/test",
            collectionId = colA.id
        )
        val bookmark = (addResult as AddBookmarkResult.Success).bookmark

        val moveSuccess = moveBookmarkUseCase(bookmark.id, colB.id)
        assertTrue(moveSuccess)

        val updatedBookmark = bookmarkRepository.getById(bookmark.id)
        assertNotNull(updatedBookmark)
        assertEquals(colB.id, updatedBookmark?.collectionId)

        // Verify other fields remain intact
        assertEquals(bookmark.url, updatedBookmark?.url)
        assertEquals(bookmark.domain, updatedBookmark?.domain)
        assertEquals(bookmark.createdAt, updatedBookmark?.createdAt)

        // Verify ColA is now empty and ColB has the bookmark
        assertEquals(0, bookmarkRepository.observeByCollection(colA.id).first().size)
        assertEquals(1, bookmarkRepository.observeByCollection(colB.id).first().size)
    }

    @Test
    fun testBookmarkDetachedFromCollection() = runBlocking {
        val col = (createCollectionUseCase(name = "Reading") as CreateCollectionResult.Success).collection
        val bookmark = (addBookmarkUseCase(
            rawUrl = "https://example.com/article",
            collectionId = col.id
        ) as AddBookmarkResult.Success).bookmark

        val detachSuccess = moveBookmarkUseCase(bookmark.id, null)
        assertTrue(detachSuccess)

        val updatedBookmark = bookmarkRepository.getById(bookmark.id)
        assertNotNull(updatedBookmark)
        assertNull(updatedBookmark?.collectionId)
        assertEquals(0, bookmarkRepository.observeByCollection(col.id).first().size)
    }

    // --- COLLECTION DELETION SAFETY TESTS ---

    @Test
    fun testDeleteCollectionPreservesBookmarksAndDetachesThem() = runBlocking {
        val col = (createCollectionUseCase(name = "Temporary") as CreateCollectionResult.Success).collection
        val bm1 = (addBookmarkUseCase("https://site1.com", collectionId = col.id) as AddBookmarkResult.Success).bookmark
        val bm2 = (addBookmarkUseCase("https://site2.com", collectionId = col.id) as AddBookmarkResult.Success).bookmark

        val deleteSuccess = deleteCollectionUseCase(col.id)
        assertTrue(deleteSuccess)

        // Collection is deleted
        assertNull(getCollectionUseCase(col.id))

        // Bookmarks still exist safely in the database with collectionId = null
        val preservedBm1 = bookmarkRepository.getById(bm1.id)
        val preservedBm2 = bookmarkRepository.getById(bm2.id)

        assertNotNull(preservedBm1)
        assertNotNull(preservedBm2)
        assertNull(preservedBm1?.collectionId)
        assertNull(preservedBm2?.collectionId)
    }

    @Test
    fun testDeleteCollectionWithSubCollectionsPreservesSubtreeBookmarks() = runBlocking {
        val parent = (createCollectionUseCase(name = "ParentFolder") as CreateCollectionResult.Success).collection
        val child = (createCollectionUseCase(name = "ChildFolder", parentId = parent.id) as CreateCollectionResult.Success).collection

        val parentBm = (addBookmarkUseCase("https://parent-link.com", collectionId = parent.id) as AddBookmarkResult.Success).bookmark
        val childBm = (addBookmarkUseCase("https://child-link.com", collectionId = child.id) as AddBookmarkResult.Success).bookmark

        // Deleting the parent folder
        val deleteSuccess = deleteCollectionUseCase(parent.id)
        assertTrue(deleteSuccess)

        // Both collections are removed
        assertNull(getCollectionUseCase(parent.id))
        assertNull(getCollectionUseCase(child.id))

        // Bookmarks in both parent and child collections are preserved!
        val preservedParentBm = bookmarkRepository.getById(parentBm.id)
        val preservedChildBm = bookmarkRepository.getById(childBm.id)

        assertNotNull(preservedParentBm)
        assertNotNull(preservedChildBm)
        assertNull(preservedParentBm?.collectionId)
        assertNull(preservedChildBm?.collectionId)
    }
}
