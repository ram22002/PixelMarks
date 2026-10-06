package com.pixelmarks.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.database.dao.BookmarkTagDao
import com.pixelmarks.core.database.dao.CollectionDao
import com.pixelmarks.core.database.dao.ReminderDao
import com.pixelmarks.core.database.dao.TagDao
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.domain.usecase.ArchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkByIdUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import com.pixelmarks.domain.usecase.ObserveTrashedBookmarksUseCase
import com.pixelmarks.domain.usecase.PermanentlyDeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.RestoreBookmarkUseCase
import com.pixelmarks.domain.usecase.SearchBookmarksUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkFavoriteUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkPinUseCase
import com.pixelmarks.domain.usecase.TrashBookmarkUseCase
import com.pixelmarks.domain.usecase.UnarchiveBookmarkUseCase
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
class BookmarkLifecycleUseCasesTest {

    private lateinit var db: PixelMarksDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var collectionDao: CollectionDao
    private lateinit var tagDao: TagDao
    private lateinit var bookmarkTagDao: BookmarkTagDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var repository: BookmarkRepository

    private lateinit var toggleFavoriteUseCase: ToggleBookmarkFavoriteUseCase
    private lateinit var togglePinUseCase: ToggleBookmarkPinUseCase
    private lateinit var archiveUseCase: ArchiveBookmarkUseCase
    private lateinit var unarchiveUseCase: UnarchiveBookmarkUseCase
    private lateinit var trashUseCase: TrashBookmarkUseCase
    private lateinit var restoreUseCase: RestoreBookmarkUseCase
    private lateinit var permanentlyDeleteUseCase: PermanentlyDeleteBookmarkUseCase
    private lateinit var observeBookmarksUseCase: ObserveBookmarksUseCase
    private lateinit var observeTrashedBookmarksUseCase: ObserveTrashedBookmarksUseCase
    private lateinit var observeBookmarkByIdUseCase: ObserveBookmarkByIdUseCase
    private lateinit var searchBookmarksUseCase: SearchBookmarksUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = PixelMarksDatabase.buildInMemory(context)
        bookmarkDao = db.bookmarkDao()
        collectionDao = db.collectionDao()
        tagDao = db.tagDao()
        bookmarkTagDao = db.bookmarkTagDao()
        reminderDao = db.reminderDao()

        repository = BookmarkRepositoryImpl(bookmarkDao)
        toggleFavoriteUseCase = ToggleBookmarkFavoriteUseCase(repository)
        togglePinUseCase = ToggleBookmarkPinUseCase(repository)
        archiveUseCase = ArchiveBookmarkUseCase(repository)
        unarchiveUseCase = UnarchiveBookmarkUseCase(repository)
        trashUseCase = TrashBookmarkUseCase(repository)
        restoreUseCase = RestoreBookmarkUseCase(repository)
        permanentlyDeleteUseCase = PermanentlyDeleteBookmarkUseCase(repository)
        observeBookmarksUseCase = ObserveBookmarksUseCase(repository)
        observeTrashedBookmarksUseCase = ObserveTrashedBookmarksUseCase(repository)
        observeBookmarkByIdUseCase = ObserveBookmarkByIdUseCase(repository)
        searchBookmarksUseCase = SearchBookmarksUseCase(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFavoriteToggleAndFieldPreservation() = runBlocking {
        val id = repository.insert(
            Bookmark(
                url = "https://example.com",
                normalizedUrl = "https://example.com",
                title = "Example",
                description = "Sample Description",
                domain = "example.com",
                notes = "Personal notes",
                isFavorite = false,
                isPinned = true
            )
        )

        // Favorite
        toggleFavoriteUseCase(id, true)
        val afterFav = repository.getById(id)
        assertNotNull(afterFav)
        assertTrue(afterFav!!.isFavorite)
        assertTrue(afterFav.isPinned) // Preserved
        assertEquals("Sample Description", afterFav.description) // Preserved
        assertEquals("Personal notes", afterFav.notes) // Preserved

        // Unfavorite
        toggleFavoriteUseCase(afterFav)
        val afterUnfav = repository.getById(id)
        assertNotNull(afterUnfav)
        assertFalse(afterUnfav!!.isFavorite)
        assertTrue(afterUnfav.isPinned)
    }

    @Test
    fun testPinToggleAndFieldPreservation() = runBlocking {
        val id = repository.insert(
            Bookmark(
                url = "https://example.com/pin",
                normalizedUrl = "https://example.com/pin",
                title = "Pinned Example",
                domain = "example.com",
                isFavorite = true,
                isPinned = false
            )
        )

        // Pin
        togglePinUseCase(id, true)
        val pinned = repository.getById(id)
        assertTrue(pinned!!.isPinned)
        assertTrue(pinned.isFavorite) // Preserved

        // Unpin
        togglePinUseCase(pinned)
        val unpinned = repository.getById(id)
        assertFalse(unpinned!!.isPinned)
        assertTrue(unpinned.isFavorite)
    }

    @Test
    fun testArchiveAndUnarchiveBehavior() = runBlocking {
        val id = repository.insert(
            Bookmark(
                url = "https://example.com/archive",
                normalizedUrl = "https://example.com/archive",
                title = "Archived Example",
                domain = "example.com",
                isArchived = false
            )
        )

        assertEquals(1, observeBookmarksUseCase().first().size)
        assertEquals(0, repository.observeArchived().first().size)

        // Archive
        archiveUseCase(id)
        val archived = repository.getById(id)
        assertTrue(archived!!.isArchived)

        // Archived must be excluded from active observeBookmarksUseCase
        assertEquals(0, observeBookmarksUseCase().first().size)
        // Archived must be visible in observeArchived
        assertEquals(1, repository.observeArchived().first().size)

        // Unarchive
        unarchiveUseCase(archived)
        val unarchived = repository.getById(id)
        assertFalse(unarchived!!.isArchived)

        // Now appears in active observeBookmarksUseCase again
        assertEquals(1, observeBookmarksUseCase().first().size)
        assertEquals(0, repository.observeArchived().first().size)
    }

    @Test
    fun testTrashAndRestoreWithFullDataPreservation() = runBlocking {
        val now = System.currentTimeMillis()
        val colId = collectionDao.insert(CollectionEntity(name = "Dev", createdAt = now, updatedAt = now))
        val tagId = tagDao.insert(TagEntity(name = "Kotlin", color = "#7F52FF", createdAt = now))

        val bookmarkId = repository.insert(
            Bookmark(
                url = "https://kotlinlang.org/docs",
                normalizedUrl = "https://kotlinlang.org/docs",
                title = "Kotlin Docs",
                description = "Documentation for Kotlin",
                domain = "kotlinlang.org",
                collectionId = colId,
                isFavorite = true,
                isPinned = true,
                notes = "Reference guide notes"
            )
        )
        bookmarkTagDao.insert(BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagId))

        // Verify initial state
        assertEquals(1, observeBookmarksUseCase().first().size)
        assertEquals(1, searchBookmarksUseCase("Kotlin").first().size)
        assertEquals(0, observeTrashedBookmarksUseCase().first().size)

        // Move to Trash
        trashUseCase(bookmarkId)

        // Excluded from active results and normal search
        assertEquals(0, observeBookmarksUseCase().first().size)
        assertEquals(0, searchBookmarksUseCase("Kotlin").first().size)

        // Visible in Trash
        val trashedList = observeTrashedBookmarksUseCase().first()
        assertEquals(1, trashedList.size)
        val trashed = trashedList.first()
        assertTrue(trashed.isTrashed)
        assertNotNull(trashed.trashedAt)

        // All fields, collection, tags, notes preserved
        assertEquals("Kotlin Docs", trashed.title)
        assertEquals("Documentation for Kotlin", trashed.description)
        assertEquals("Reference guide notes", trashed.notes)
        assertEquals(colId, trashed.collectionId)
        assertTrue(trashed.isFavorite)
        assertTrue(trashed.isPinned)
        val tagsForBookmark = bookmarkTagDao.observeTagsForBookmark(bookmarkId).first()
        assertEquals(1, tagsForBookmark.size)
        assertEquals("Kotlin", tagsForBookmark.first().name)

        // Restore from Trash
        restoreUseCase(bookmarkId)

        // Verify restoration into active results
        assertEquals(1, observeBookmarksUseCase().first().size)
        assertEquals(1, searchBookmarksUseCase("Kotlin").first().size)
        assertEquals(0, observeTrashedBookmarksUseCase().first().size)

        val restored = repository.getById(bookmarkId)
        assertNotNull(restored)
        assertFalse(restored!!.isTrashed)
        assertNull(restored.trashedAt)
        assertTrue(restored.isFavorite)
        assertTrue(restored.isPinned)
        assertEquals(colId, restored.collectionId)
        assertEquals("Reference guide notes", restored.notes)
    }

    @Test
    fun testPermanentDeleteCascadesCrossRefsAndRemindersWithoutDeletingTagsOrCollections() = runBlocking {
        val now = System.currentTimeMillis()
        val colId = collectionDao.insert(CollectionEntity(name = "Articles", createdAt = now, updatedAt = now))
        val tagId = tagDao.insert(TagEntity(name = "Tech", color = "#000000", createdAt = now))

        val bookmarkId = repository.insert(
            Bookmark(
                url = "https://techcrunch.com",
                normalizedUrl = "https://techcrunch.com",
                title = "TechCrunch",
                domain = "techcrunch.com",
                collectionId = colId
            )
        )
        bookmarkTagDao.insert(BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagId))
        val reminderId = reminderDao.insert(
            ReminderEntity(
                bookmarkId = bookmarkId,
                reminderAt = now + 10000,
                createdAt = now,
                updatedAt = now
            )
        )

        // Verify associations exist
        assertNotNull(repository.getById(bookmarkId))
        assertEquals(1, bookmarkTagDao.observeTagsForBookmark(bookmarkId).first().size)
        assertNotNull(reminderDao.getById(reminderId))

        // Permanently delete bookmark
        permanentlyDeleteUseCase(bookmarkId)

        // Bookmark is gone
        assertNull(repository.getById(bookmarkId))

        // BookmarkTagCrossRef is cascaded and gone
        assertEquals(0, bookmarkTagDao.observeTagsForBookmark(bookmarkId).first().size)

        // Reminder is cascaded and gone
        assertNull(reminderDao.getById(reminderId))

        // Independent entities (Tag and Collection) MUST remain intact
        val preservedTag = tagDao.getById(tagId)
        assertNotNull("Tag must not be deleted when bookmark is permanently deleted", preservedTag)
        assertEquals("Tech", preservedTag?.name)

        val preservedCollection = collectionDao.getById(colId)
        assertNotNull("Collection must not be deleted when bookmark is permanently deleted", preservedCollection)
        assertEquals("Articles", preservedCollection?.name)
    }

    @Test
    fun testReactiveEmissionsOnLifecycleChanges() = runBlocking {
        val id = repository.insert(
            Bookmark(
                url = "https://reactive.org",
                normalizedUrl = "https://reactive.org",
                title = "Reactive",
                domain = "reactive.org"
            )
        )

        // Check observeBookmarkByIdUseCase
        val initial = observeBookmarkByIdUseCase(id).first()
        assertFalse(initial!!.isFavorite)

        // Toggle Favorite
        toggleFavoriteUseCase(id, true)
        val favEmitted = observeBookmarkByIdUseCase(id).first()
        assertTrue(favEmitted!!.isFavorite)

        // Move to Trash
        trashUseCase(id)
        val trashEmitted = observeBookmarkByIdUseCase(id).first()
        assertTrue(trashEmitted!!.isTrashed)
        assertEquals(0, observeBookmarksUseCase().first().size)
        assertEquals(1, observeTrashedBookmarksUseCase().first().size)

        // Restore
        restoreUseCase(id)
        val restoredEmitted = observeBookmarkByIdUseCase(id).first()
        assertFalse(restoredEmitted!!.isTrashed)
        assertEquals(1, observeBookmarksUseCase().first().size)
        assertEquals(0, observeTrashedBookmarksUseCase().first().size)
    }

    @Test
    fun testSearchExcludesTrashedInBothRegularAndArchivedModes() = runBlocking {
        val idActive = repository.insert(
            Bookmark(
                url = "https://active.com",
                normalizedUrl = "https://active.com",
                title = "Common Search Query",
                domain = "active.com",
                isArchived = false,
                isTrashed = false
            )
        )
        val idArchived = repository.insert(
            Bookmark(
                url = "https://archived.com",
                normalizedUrl = "https://archived.com",
                title = "Common Search Query",
                domain = "archived.com",
                isArchived = true,
                isTrashed = false
            )
        )
        val idTrashed = repository.insert(
            Bookmark(
                url = "https://trashed.com",
                normalizedUrl = "https://trashed.com",
                title = "Common Search Query",
                domain = "trashed.com",
                isArchived = false,
                isTrashed = true
            )
        )

        // Regular search: excludes archived and excludes trashed
        val regularResults = searchBookmarksUseCase(
            query = "Common",
            filters = BookmarkFilters(archivedOnly = false)
        ).first()
        assertEquals(1, regularResults.size)
        assertEquals(idActive, regularResults.first().id)

        // Archived filter search: includes archived, excludes trashed
        val archivedResults = searchBookmarksUseCase(
            query = "Common",
            filters = BookmarkFilters(archivedOnly = true)
        ).first()
        assertEquals(1, archivedResults.size)
        assertEquals(idArchived, archivedResults.first().id)
    }
}
