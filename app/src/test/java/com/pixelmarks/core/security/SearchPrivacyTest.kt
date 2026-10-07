package com.pixelmarks.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.database.dao.BookmarkTagDao
import com.pixelmarks.core.database.dao.CollectionDao
import com.pixelmarks.core.database.dao.TagDao
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
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
class SearchPrivacyTest {

    private lateinit var db: PixelMarksDatabase
    private lateinit var bookmarkDao: BookmarkDao
    private lateinit var collectionDao: CollectionDao
    private lateinit var tagDao: TagDao
    private lateinit var bookmarkTagDao: BookmarkTagDao
    private lateinit var bookmarkRepository: BookmarkRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = PixelMarksDatabase.buildInMemory(context)
        bookmarkDao = db.bookmarkDao()
        collectionDao = db.collectionDao()
        tagDao = db.tagDao()
        bookmarkTagDao = db.bookmarkTagDao()
        bookmarkRepository = BookmarkRepositoryImpl(bookmarkDao)
        tagRepository = TagRepositoryImpl(tagDao, bookmarkTagDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun hiddenBookmark_excludedFromNormalSearch_includedWhenPrivateSessionActive() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Create a regular collection
        val publicColId = collectionDao.insert(
            CollectionEntity(
                name = "Public Tech",
                icon = "folder",
                color = "#3B82F6",
                isLocked = false,
                isHidden = false,
                createdAt = now,
                updatedAt = now
            )
        )

        // 2. Create a hidden collection
        val hiddenColId = collectionDao.insert(
            CollectionEntity(
                name = "Private Vault",
                icon = "folder",
                color = "#EF4444",
                isLocked = true,
                isHidden = true,
                createdAt = now,
                updatedAt = now
            )
        )

        // 3. Add public bookmark
        bookmarkDao.insert(
            BookmarkEntity(
                url = "https://public.dev/react",
                normalizedUrl = "https://public.dev/react",
                domain = "public.dev",
                title = "React Development Tips",
                description = "Useful React snippets",
                collectionId = publicColId,
                createdAt = now,
                updatedAt = now
            )
        )

        // 4. Add private/hidden bookmark
        bookmarkDao.insert(
            BookmarkEntity(
                url = "https://confidential.corp/secrets",
                normalizedUrl = "https://confidential.corp/secrets",
                domain = "confidential.corp",
                title = "Confidential Banking Passwords",
                description = "Critical personal tokens",
                notes = "Bank account numbers",
                collectionId = hiddenColId,
                isPrivate = true,
                createdAt = now,
                updatedAt = now
            )
        )

        // Query: Search for "Bank" or "Pass"
        val normalSearchResults = bookmarkRepository.observeBookmarks(
            query = "Bank",
            filters = BookmarkFilters(),
            sort = BookmarkSortOption.NEWEST,
            isPrivate = false
        ).first()

        // Must NOT leak through normal search
        assertEquals(0, normalSearchResults.size)

        // Search when private session is authenticated (isPrivate = true)
        val privateSearchResults = bookmarkRepository.observeBookmarks(
            query = "Bank",
            filters = BookmarkFilters(),
            sort = BookmarkSortOption.NEWEST,
            isPrivate = true
        ).first()

        assertEquals(1, privateSearchResults.size)
        assertEquals("Confidential Banking Passwords", privateSearchResults.first().title)
    }

    @Test
    fun hiddenCollection_excludedFromTagResults() = runBlocking {
        val now = System.currentTimeMillis()

        val hiddenColId = collectionDao.insert(
            CollectionEntity(
                name = "Secret Stash",
                icon = "folder",
                color = "#10B981",
                isLocked = true,
                isHidden = true,
                createdAt = now,
                updatedAt = now
            )
        )

        val tagId = tagDao.insert(
            TagEntity(name = "finance", color = "#F59E0B", createdAt = now)
        )

        val bmId = bookmarkDao.insert(
            BookmarkEntity(
                url = "https://crypto.wallet/keys",
                normalizedUrl = "https://crypto.wallet/keys",
                domain = "crypto.wallet",
                title = "Crypto Private Keys",
                collectionId = hiddenColId,
                isPrivate = true,
                createdAt = now,
                updatedAt = now
            )
        )

        bookmarkTagDao.insert(BookmarkTagCrossRef(bookmarkId = bmId, tagId = tagId))

        // Normal tag query must EXCLUDE bookmarks from hidden collections
        val tagBookmarksNormal = tagRepository.observeBookmarksForTag(tagId, isPrivate = false).first()
        assertEquals(0, tagBookmarksNormal.size)

        // Authenticated private session INCLUDES them
        val tagBookmarksPrivate = tagRepository.observeBookmarksForTag(tagId, isPrivate = true).first()
        assertEquals(1, tagBookmarksPrivate.size)
        assertEquals("Crypto Private Keys", tagBookmarksPrivate.first().title)
    }

    @Test
    fun hiddenCollection_excludedFromHomeResultsAndTotalCount() = runBlocking {
        val now = System.currentTimeMillis()

        val hiddenColId = collectionDao.insert(
            CollectionEntity(
                name = "Stealth Folder",
                icon = "folder",
                color = "#8B5CF6",
                isLocked = true,
                isHidden = true,
                isPrivate = true,
                createdAt = now,
                updatedAt = now
            )
        )

        bookmarkDao.insert(
            BookmarkEntity(
                url = "https://stealth.org/doc",
                normalizedUrl = "https://stealth.org/doc",
                domain = "stealth.org",
                title = "Secret Whitepaper",
                collectionId = hiddenColId,
                isPrivate = true,
                createdAt = now,
                updatedAt = now
            )
        )

        bookmarkDao.insert(
            BookmarkEntity(
                url = "https://public.org/doc",
                normalizedUrl = "https://public.org/doc",
                domain = "public.org",
                title = "Public Blog",
                collectionId = null,
                isPrivate = false,
                createdAt = now,
                updatedAt = now
            )
        )

        val homeBookmarks = bookmarkRepository.observeAll(isPrivate = false).first()
        assertEquals(1, homeBookmarks.size)
        assertEquals("Public Blog", homeBookmarks.first().title)

        val totalCountNormal = bookmarkRepository.observeTotalCount(isPrivate = false).first()
        assertEquals(1, totalCountNormal)

        val totalCountWithHidden = bookmarkRepository.observeTotalCount(isPrivate = true).first()
        assertEquals(1, totalCountWithHidden)
    }
}
