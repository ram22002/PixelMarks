package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Bookmark ↔ Tag many-to-many relationship operations.
 */
@Dao
interface BookmarkTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: BookmarkTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(crossRefs: List<BookmarkTagCrossRef>)

    @Delete
    suspend fun delete(crossRef: BookmarkTagCrossRef)

    @Query("DELETE FROM bookmark_tag_cross_ref WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: Long)

    @Query("DELETE FROM bookmark_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: Long)

    @Query("SELECT * FROM bookmark_tag_cross_ref")
    fun observeAllCrossRefs(): Flow<List<BookmarkTagCrossRef>>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN bookmark_tag_cross_ref ref ON t.id = ref.tagId
        WHERE ref.bookmarkId = :bookmarkId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsForBookmark(bookmarkId: Long): List<TagEntity>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN bookmark_tag_cross_ref ref ON t.id = ref.tagId
        WHERE ref.bookmarkId = :bookmarkId
        ORDER BY t.name ASC
    """)
    fun observeTagsForBookmark(bookmarkId: Long): Flow<List<TagEntity>>

    @Query("""
        SELECT b.* FROM bookmarks b
        INNER JOIN bookmark_tag_cross_ref ref ON b.id = ref.bookmarkId
        WHERE ref.tagId = :tagId AND b.isTrashed = 0
        ORDER BY b.isPinned DESC, b.createdAt DESC
    """)
    suspend fun getBookmarksForTag(tagId: Long): List<BookmarkEntity>

    @Query("""
        SELECT b.* FROM bookmarks b
        INNER JOIN bookmark_tag_cross_ref ref ON b.id = ref.bookmarkId
        WHERE ref.tagId = :tagId AND b.isTrashed = 0
        ORDER BY b.isPinned DESC, b.createdAt DESC
    """)
    fun observeBookmarksForTag(tagId: Long): Flow<List<BookmarkEntity>>
}
