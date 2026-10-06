package com.pixelmarks.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.Tag
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchBookmarksTest {

    private lateinit var database: PixelMarksDatabase
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var searchBookmarksUseCase: SearchBookmarksUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = PixelMarksDatabase.buildInMemory(context)
        bookmarkRepository = BookmarkRepositoryImpl(database.bookmarkDao())
        collectionRepository = CollectionRepositoryImpl(database.collectionDao())
        tagRepository = TagRepositoryImpl(database.tagDao(), database.bookmarkTagDao())
        searchBookmarksUseCase = SearchBookmarksUseCase(bookmarkRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertBookmark(
        title: String,
        url: String,
        domain: String = "example.com",
        description: String? = null,
        notes: String? = null,
        collectionId: Long? = null,
        isFavorite: Boolean = false,
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
        lastOpenedAt: Long? = null,
        visitCount: Int = 0
    ): Long {
        return bookmarkRepository.insert(
            Bookmark(
                url = url,
                normalizedUrl = url,
                title = title,
                description = description,
                domain = domain,
                collectionId = collectionId,
                isFavorite = isFavorite,
                isPinned = isPinned,
                isArchived = isArchived,
                notes = notes,
                createdAt = createdAt,
                updatedAt = updatedAt,
                lastOpenedAt = lastOpenedAt,
                visitCount = visitCount
            )
        )
    }

    // ==========================================
    // Query Matching Tests
    // ==========================================

    @Test
    fun testEmptyQueryReturnsActiveBookmarks() = runBlocking {
        insertBookmark(title = "Google", url = "https://google.com")
        insertBookmark(title = "GitHub", url = "https://github.com")
        insertBookmark(title = "Archived Item", url = "https://archived.com", isArchived = true)

        val results = searchBookmarksUseCase(query = "").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.title == "Google" })
        assertTrue(results.any { it.title == "GitHub" })
        assertFalse(results.any { it.title == "Archived Item" })
    }

    @Test
    fun testTitleMatchPartialAndCaseInsensitive() = runBlocking {
        insertBookmark(title = "Android Developers Guide", url = "https://developer.android.com")
        insertBookmark(title = "Kotlin Documentation", url = "https://kotlinlang.org")

        val results = searchBookmarksUseCase(query = "android").first()
        assertEquals(1, results.size)
        assertEquals("Android Developers Guide", results.first().title)

        val upperResults = searchBookmarksUseCase(query = "ANDR").first()
        assertEquals(1, upperResults.size)
        assertEquals("Android Developers Guide", upperResults.first().title)
    }

    @Test
    fun testUrlMatchPartialAndCaseInsensitive() = runBlocking {
        insertBookmark(title = "My Docs", url = "https://docs.oracle.com/javase")
        insertBookmark(title = "General Search", url = "https://google.com")

        val results = searchBookmarksUseCase(query = "oracle.com/java").first()
        assertEquals(1, results.size)
        assertEquals("My Docs", results.first().title)
    }

    @Test
    fun testDomainMatch() = runBlocking {
        insertBookmark(title = "Dev Portal", url = "https://subdomain.example.org/path", domain = "subdomain.example.org")
        insertBookmark(title = "Other Site", url = "https://another.com", domain = "another.com")

        val results = searchBookmarksUseCase(query = "example.org").first()
        assertEquals(1, results.size)
        assertEquals("Dev Portal", results.first().title)
    }

    @Test
    fun testDescriptionMatch() = runBlocking {
        insertBookmark(title = "Site A", url = "https://sitea.com", description = "Everything about microservices architecture")
        insertBookmark(title = "Site B", url = "https://siteb.com", description = "Monolithic design principles")

        val results = searchBookmarksUseCase(query = "microservices").first()
        assertEquals(1, results.size)
        assertEquals("Site A", results.first().title)
    }

    @Test
    fun testNotesMatch() = runBlocking {
        insertBookmark(title = "Article 1", url = "https://a1.com", notes = "Review before team sprint planning meeting")
        insertBookmark(title = "Article 2", url = "https://a2.com", notes = "Unrelated read")

        val results = searchBookmarksUseCase(query = "sprint planning").first()
        assertEquals(1, results.size)
        assertEquals("Article 1", results.first().title)
    }

    @Test
    fun testTagMatchPartialAndCaseInsensitive() = runBlocking {
        val b1 = insertBookmark(title = "Article One", url = "https://one.com")
        val b2 = insertBookmark(title = "Article Two", url = "https://two.com")

        val tagId = tagRepository.insert(Tag(name = "Jetpack Compose"))
        tagRepository.addTagToBookmark(b1, tagId)

        val results = searchBookmarksUseCase(query = "compose").first()
        assertEquals(1, results.size)
        assertEquals(b1, results.first().id)

        val caseResults = searchBookmarksUseCase(query = "JETPACK").first()
        assertEquals(1, caseResults.size)
        assertEquals(b1, caseResults.first().id)
    }

    @Test
    fun testWhitespaceTrimmingOnQuery() = runBlocking {
        insertBookmark(title = "Jetpack Navigation", url = "https://nav.com")

        val results = searchBookmarksUseCase(query = "   navigation   ").first()
        assertEquals(1, results.size)
        assertEquals("Jetpack Navigation", results.first().title)

        val blankResults = searchBookmarksUseCase(query = "    ").first()
        assertEquals(1, blankResults.size)
    }

    @Test
    fun testQueryWithNoMatchingResults() = runBlocking {
        insertBookmark(title = "Kotlin", url = "https://kotlinlang.org")

        val results = searchBookmarksUseCase(query = "nonexistent_query_xyz").first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun testQueryWithSpecialCharactersWildcards() = runBlocking {
        insertBookmark(title = "100% Guaranteed_Result\\Guide", url = "https://example.com/special")
        insertBookmark(title = "1000 Guaranteed Results Guide", url = "https://example.com/other")

        // Literal % match
        val pctResults = searchBookmarksUseCase(query = "100%").first()
        assertEquals(1, pctResults.size)
        assertEquals("100% Guaranteed_Result\\Guide", pctResults.first().title)

        // Literal _ match
        val underscoreResults = searchBookmarksUseCase(query = "Result_Guide").first()
        assertEquals(0, underscoreResults.size) // Doesn't match because exact is Result\Guide

        val exactUnder = searchBookmarksUseCase(query = "Guaranteed_Result").first()
        assertEquals(1, exactUnder.size)
    }

    // ==========================================
    // Filter Tests
    // ==========================================

    @Test
    fun testFavoriteOnlyFilter() = runBlocking {
        insertBookmark(title = "Fav Bookmark", url = "https://fav.com", isFavorite = true)
        insertBookmark(title = "Normal Bookmark", url = "https://normal.com", isFavorite = false)

        val results = searchBookmarksUseCase(filters = BookmarkFilters(favoriteOnly = true)).first()
        assertEquals(1, results.size)
        assertEquals("Fav Bookmark", results.first().title)
    }

    @Test
    fun testPinnedOnlyFilter() = runBlocking {
        insertBookmark(title = "Pinned Item", url = "https://pinned.com", isPinned = true)
        insertBookmark(title = "Unpinned Item", url = "https://unpinned.com", isPinned = false)

        val results = searchBookmarksUseCase(filters = BookmarkFilters(pinnedOnly = true)).first()
        assertEquals(1, results.size)
        assertEquals("Pinned Item", results.first().title)
    }

    @Test
    fun testArchivedOnlyFilter() = runBlocking {
        insertBookmark(title = "Active Item", url = "https://active.com", isArchived = false)
        insertBookmark(title = "Archived Item", url = "https://archived.com", isArchived = true)

        val results = searchBookmarksUseCase(filters = BookmarkFilters(archivedOnly = true)).first()
        assertEquals(1, results.size)
        assertEquals("Archived Item", results.first().title)
    }

    @Test
    fun testCollectionFilter() = runBlocking {
        val colId = collectionRepository.insert(Collection(name = "Android"))
        insertBookmark(title = "In Collection", url = "https://col.com", collectionId = colId)
        insertBookmark(title = "No Collection", url = "https://none.com")

        val results = searchBookmarksUseCase(filters = BookmarkFilters(collectionId = colId)).first()
        assertEquals(1, results.size)
        assertEquals("In Collection", results.first().title)
    }

    @Test
    fun testTagIdFilter() = runBlocking {
        val b1 = insertBookmark(title = "Tag 1 Bookmark", url = "https://b1.com")
        val b2 = insertBookmark(title = "Tag 2 Bookmark", url = "https://b2.com")

        val tagId1 = tagRepository.insert(Tag(name = "Kotlin"))
        val tagId2 = tagRepository.insert(Tag(name = "Android"))

        tagRepository.addTagToBookmark(b1, tagId1)
        tagRepository.addTagToBookmark(b2, tagId2)

        val results = searchBookmarksUseCase(filters = BookmarkFilters(tagId = tagId1)).first()
        assertEquals(1, results.size)
        assertEquals(b1, results.first().id)
    }

    @Test
    fun testComposableFiltersCombined() = runBlocking {
        val colId = collectionRepository.insert(Collection(name = "Tech"))
        val tagId = tagRepository.insert(Tag(name = "Mobile"))

        // Matches all: favorite + pinned + collection + tag
        val bMatch = insertBookmark(
            title = "Match All",
            url = "https://match.com",
            isFavorite = true,
            isPinned = true,
            collectionId = colId
        )
        tagRepository.addTagToBookmark(bMatch, tagId)

        // Matches collection + tag + favorite, but NOT pinned
        val bNotPinned = insertBookmark(
            title = "Not Pinned",
            url = "https://notpinned.com",
            isFavorite = true,
            isPinned = false,
            collectionId = colId
        )
        tagRepository.addTagToBookmark(bNotPinned, tagId)

        val results = searchBookmarksUseCase(
            filters = BookmarkFilters(
                favoriteOnly = true,
                pinnedOnly = true,
                collectionId = colId,
                tagId = tagId
            )
        ).first()

        assertEquals(1, results.size)
        assertEquals(bMatch, results.first().id)
    }

    // ==========================================
    // Sorting Tests
    // ==========================================

    @Test
    fun testSortNewestAndOldest() = runBlocking {
        insertBookmark(title = "Old", url = "https://old.com", createdAt = 1000L)
        insertBookmark(title = "New", url = "https://new.com", createdAt = 2000L)

        val newest = searchBookmarksUseCase(sort = BookmarkSortOption.NEWEST).first()
        assertEquals("New", newest[0].title)
        assertEquals("Old", newest[1].title)

        val oldest = searchBookmarksUseCase(sort = BookmarkSortOption.OLDEST).first()
        assertEquals("Old", oldest[0].title)
        assertEquals("New", oldest[1].title)
    }

    @Test
    fun testSortTitleCaseInsensitiveAndDeterministic() = runBlocking {
        insertBookmark(title = "banana", url = "https://b.com")
        insertBookmark(title = "Apple", url = "https://a.com")
        insertBookmark(title = "cherry", url = "https://c.com")

        val sorted = searchBookmarksUseCase(sort = BookmarkSortOption.TITLE).first()
        assertEquals("Apple", sorted[0].title)
        assertEquals("banana", sorted[1].title)
        assertEquals("cherry", sorted[2].title)
    }

    @Test
    fun testSortRecentlyOpenedNullsLast() = runBlocking {
        insertBookmark(title = "Opened Yesterday", url = "https://1.com", lastOpenedAt = 10000L)
        insertBookmark(title = "Opened Today", url = "https://2.com", lastOpenedAt = 20000L)
        insertBookmark(title = "Never Opened", url = "https://3.com", lastOpenedAt = null)

        val sorted = searchBookmarksUseCase(sort = BookmarkSortOption.RECENTLY_OPENED).first()
        assertEquals("Opened Today", sorted[0].title)
        assertEquals("Opened Yesterday", sorted[1].title)
        assertEquals("Never Opened", sorted[2].title)
    }

    @Test
    fun testSortMostVisited() = runBlocking {
        insertBookmark(title = "Visit 5", url = "https://5.com", visitCount = 5)
        insertBookmark(title = "Visit 100", url = "https://100.com", visitCount = 100)
        insertBookmark(title = "Visit 0", url = "https://0.com", visitCount = 0)

        val sorted = searchBookmarksUseCase(sort = BookmarkSortOption.MOST_VISITED).first()
        assertEquals("Visit 100", sorted[0].title)
        assertEquals("Visit 5", sorted[1].title)
        assertEquals("Visit 0", sorted[2].title)
    }

    @Test
    fun testSortRecentlyModified() = runBlocking {
        insertBookmark(title = "Mod First", url = "https://1.com", updatedAt = 5000L)
        insertBookmark(title = "Mod Latest", url = "https://2.com", updatedAt = 9000L)

        val sorted = searchBookmarksUseCase(sort = BookmarkSortOption.RECENTLY_MODIFIED).first()
        assertEquals("Mod Latest", sorted[0].title)
        assertEquals("Mod First", sorted[1].title)
    }

    // ==========================================
    // Combination Tests (Query + Filters)
    // ==========================================

    @Test
    fun testCombinedQueryAndTag() = runBlocking {
        val b1 = insertBookmark(title = "Android Guide", url = "https://guide.com")
        val b2 = insertBookmark(title = "Android Reference", url = "https://ref.com")

        val tagId = tagRepository.insert(Tag(name = "Kotlin"))
        tagRepository.addTagToBookmark(b1, tagId)

        val results = searchBookmarksUseCase(
            query = "Android",
            filters = BookmarkFilters(tagId = tagId)
        ).first()

        assertEquals(1, results.size)
        assertEquals(b1, results.first().id)
    }

    @Test
    fun testCombinedQueryAndCollection() = runBlocking {
        val colId1 = collectionRepository.insert(Collection(name = "Col 1"))
        val colId2 = collectionRepository.insert(Collection(name = "Col 2"))

        insertBookmark(title = "Android 1", url = "https://a1.com", collectionId = colId1)
        insertBookmark(title = "Android 2", url = "https://a2.com", collectionId = colId2)

        val results = searchBookmarksUseCase(
            query = "Android",
            filters = BookmarkFilters(collectionId = colId1)
        ).first()

        assertEquals(1, results.size)
        assertEquals("Android 1", results.first().title)
    }

    @Test
    fun testCombinedQueryAndFavorite() = runBlocking {
        insertBookmark(title = "Compose Layout", url = "https://1.com", isFavorite = true)
        insertBookmark(title = "Compose State", url = "https://2.com", isFavorite = false)

        val results = searchBookmarksUseCase(
            query = "Compose",
            filters = BookmarkFilters(favoriteOnly = true)
        ).first()

        assertEquals(1, results.size)
        assertEquals("Compose Layout", results.first().title)
    }

    @Test
    fun testCombinedQueryTagAndFavorite() = runBlocking {
        val tagId = tagRepository.insert(Tag(name = "Docs"))

        val bMatch = insertBookmark(title = "Architecture Guide", url = "https://arch.com", isFavorite = true)
        val bNoFav = insertBookmark(title = "Architecture Book", url = "https://book.com", isFavorite = false)

        tagRepository.addTagToBookmark(bMatch, tagId)
        tagRepository.addTagToBookmark(bNoFav, tagId)

        val results = searchBookmarksUseCase(
            query = "Architecture",
            filters = BookmarkFilters(tagId = tagId, favoriteOnly = true)
        ).first()

        assertEquals(1, results.size)
        assertEquals(bMatch, results.first().id)
    }

    @Test
    fun testCombinedQueryCollectionAndTag() = runBlocking {
        val colId = collectionRepository.insert(Collection(name = "Tutorials"))
        val tagId = tagRepository.insert(Tag(name = "Advanced"))

        val b1 = insertBookmark(title = "Coroutines Flow", url = "https://flow.com", collectionId = colId)
        tagRepository.addTagToBookmark(b1, tagId)

        val b2 = insertBookmark(title = "Coroutines Channel", url = "https://ch.com", collectionId = colId)
        // b2 does not have the tag

        val results = searchBookmarksUseCase(
            query = "Coroutines",
            filters = BookmarkFilters(collectionId = colId, tagId = tagId)
        ).first()

        assertEquals(1, results.size)
        assertEquals(b1, results.first().id)
    }

    // ==========================================
    // Reactive Behavior Tests
    // ==========================================

    @Test
    fun testReactiveUpdatesOnBookmarkCreation() = runBlocking {
        val initial = searchBookmarksUseCase(query = "Reactive").first()
        assertEquals(0, initial.size)

        insertBookmark(title = "Reactive Programming", url = "https://reactive.com")

        val afterInsert = searchBookmarksUseCase(query = "Reactive").first()
        assertEquals(1, afterInsert.size)
        assertEquals("Reactive Programming", afterInsert.first().title)
    }

    @Test
    fun testReactiveUpdatesOnBookmarkEdit() = runBlocking {
        val id = insertBookmark(title = "Initial Title", url = "https://test.com")

        val before = searchBookmarksUseCase(query = "Updated").first()
        assertEquals(0, before.size)

        val existing = bookmarkRepository.getById(id)!!
        bookmarkRepository.update(existing.copy(title = "Updated New Title"))

        val after = searchBookmarksUseCase(query = "Updated").first()
        assertEquals(1, after.size)
        assertEquals(id, after.first().id)
    }

    @Test
    fun testReactiveUpdatesOnBookmarkDeletion() = runBlocking {
        val id = insertBookmark(title = "To Delete", url = "https://del.com")

        val before = searchBookmarksUseCase(query = "Delete").first()
        assertEquals(1, before.size)

        bookmarkRepository.deleteById(id)

        val after = searchBookmarksUseCase(query = "Delete").first()
        assertEquals(0, after.size)
    }

    @Test
    fun testReactiveUpdatesOnFavoriteToggle() = runBlocking {
        val id = insertBookmark(title = "Fav Target", url = "https://fav.com", isFavorite = false)

        val before = searchBookmarksUseCase(filters = BookmarkFilters(favoriteOnly = true)).first()
        assertEquals(0, before.size)

        val existing = bookmarkRepository.getById(id)!!
        bookmarkRepository.update(existing.copy(isFavorite = true))

        val after = searchBookmarksUseCase(filters = BookmarkFilters(favoriteOnly = true)).first()
        assertEquals(1, after.size)
    }

    @Test
    fun testReactiveUpdatesOnPinToggle() = runBlocking {
        val id = insertBookmark(title = "Pin Target", url = "https://pin.com", isPinned = false)

        val before = searchBookmarksUseCase(filters = BookmarkFilters(pinnedOnly = true)).first()
        assertEquals(0, before.size)

        val existing = bookmarkRepository.getById(id)!!
        bookmarkRepository.update(existing.copy(isPinned = true))

        val after = searchBookmarksUseCase(filters = BookmarkFilters(pinnedOnly = true)).first()
        assertEquals(1, after.size)
    }

    @Test
    fun testReactiveUpdatesOnArchiveToggle() = runBlocking {
        val id = insertBookmark(title = "Archive Target", url = "https://arch.com", isArchived = false)

        val activeBefore = searchBookmarksUseCase().first()
        assertEquals(1, activeBefore.size)

        val existing = bookmarkRepository.getById(id)!!
        bookmarkRepository.update(existing.copy(isArchived = true))

        val activeAfter = searchBookmarksUseCase().first()
        assertEquals(0, activeAfter.size)

        val archivedAfter = searchBookmarksUseCase(filters = BookmarkFilters(archivedOnly = true)).first()
        assertEquals(1, archivedAfter.size)
    }

    @Test
    fun testReactiveUpdatesOnCollectionChange() = runBlocking {
        val colId = collectionRepository.insert(Collection(name = "TargetCol"))
        val id = insertBookmark(title = "Moving Bookmark", url = "https://move.com", collectionId = null)

        val before = searchBookmarksUseCase(filters = BookmarkFilters(collectionId = colId)).first()
        assertEquals(0, before.size)

        val existing = bookmarkRepository.getById(id)!!
        bookmarkRepository.update(existing.copy(collectionId = colId))

        val after = searchBookmarksUseCase(filters = BookmarkFilters(collectionId = colId)).first()
        assertEquals(1, after.size)
    }

    @Test
    fun testReactiveUpdatesOnTagAssignmentAndRemoval() = runBlocking {
        val tagId = tagRepository.insert(Tag(name = "SpecialTag"))
        val id = insertBookmark(title = "Tag Reactive", url = "https://tag.com")

        val before = searchBookmarksUseCase(filters = BookmarkFilters(tagId = tagId)).first()
        assertEquals(0, before.size)

        // Assign tag
        tagRepository.addTagToBookmark(id, tagId)
        val afterAdd = searchBookmarksUseCase(filters = BookmarkFilters(tagId = tagId)).first()
        assertEquals(1, afterAdd.size)

        // Query by tag name
        val queryByName = searchBookmarksUseCase(query = "SpecialTag").first()
        assertEquals(1, queryByName.size)

        // Remove tag
        tagRepository.removeTagFromBookmark(id, tagId)
        val afterRemove = searchBookmarksUseCase(filters = BookmarkFilters(tagId = tagId)).first()
        assertEquals(0, afterRemove.size)
    }

    @Test
    fun testObserveTotalCount() = runBlocking {
        assertEquals(0, searchBookmarksUseCase.observeTotalCount().first())

        insertBookmark(title = "B1", url = "https://b1.com")
        insertBookmark(title = "B2", url = "https://b2.com")

        assertEquals(2, searchBookmarksUseCase.observeTotalCount().first())
    }
}
