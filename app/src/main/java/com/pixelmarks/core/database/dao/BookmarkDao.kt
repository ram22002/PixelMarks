package com.pixelmarks.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pixelmarks.core.database.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Bookmark operations.
 */
@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>): List<Long>

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun observeById(id: Long): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks WHERE normalizedUrl = :normalizedUrl")
    suspend fun getByNormalizedUrl(normalizedUrl: String): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE collectionId = :collectionId AND isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observeByCollection(collectionId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE collectionId IS NULL AND isArchived = 0 AND isTrashed = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun observeUnsorted(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isFavorite = 1 AND isArchived = 0 AND isTrashed = 0 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isPinned = 1 AND isArchived = 0 AND isTrashed = 0 ORDER BY createdAt DESC")
    fun observePinned(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE isArchived = 1 AND isTrashed = 0 ORDER BY createdAt DESC")
    fun observeArchived(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE domain = :domain AND isArchived = 0 AND isTrashed = 0 ORDER BY createdAt DESC")
    fun observeByDomain(domain: String): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE isTrashed = 0")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM bookmarks WHERE isTrashed = 1 ORDER BY trashedAt DESC, createdAt DESC")
    fun observeTrashed(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE isTrashed = 1")
    fun observeTrashedCount(): Flow<Int>

    @Query("UPDATE bookmarks SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isTrashed = 1, trashedAt = :trashedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, trashedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bookmarks SET isTrashed = 0, trashedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        SELECT DISTINCT b.* FROM bookmarks b
        LEFT JOIN bookmark_tag_cross_ref ref ON b.id = ref.bookmarkId
        LEFT JOIN tags t ON ref.tagId = t.id
        WHERE b.isArchived = :archivedOnly
          AND b.isTrashed = 0
          AND (:favoriteOnly = 0 OR b.isFavorite = 1)
          AND (:pinnedOnly = 0 OR b.isPinned = 1)
          AND (:collectionId IS NULL OR b.collectionId = :collectionId)
          AND (:tagId IS NULL OR b.id IN (SELECT bookmarkId FROM bookmark_tag_cross_ref WHERE tagId = :tagId))
          AND (
            :query = '' OR
            LOWER(b.title) LIKE '%' || LOWER(:escapedQuery) || '%' ESCAPE '\' OR
            LOWER(b.url) LIKE '%' || LOWER(:escapedQuery) || '%' ESCAPE '\' OR
            LOWER(b.domain) LIKE '%' || LOWER(:escapedQuery) || '%' ESCAPE '\' OR
            (b.description IS NOT NULL AND LOWER(b.description) LIKE '%' || LOWER(:escapedQuery) || '%' ESCAPE '\') OR
            (b.notes IS NOT NULL AND LOWER(b.notes) LIKE '%' || LOWER(:escapedQuery) || '%' ESCAPE '\') OR
            (t.name IS NOT NULL AND LOWER(t.name) LIKE '%' || LOWER(:escapedQuery) || '%' ESCAPE '\')
          )
        ORDER BY b.isPinned DESC, b.createdAt DESC
    """)
    fun searchBookmarks(
        query: String,
        escapedQuery: String,
        favoriteOnly: Boolean,
        pinnedOnly: Boolean,
        archivedOnly: Boolean,
        collectionId: Long?,
        tagId: Long?
    ): Flow<List<BookmarkEntity>>

    @Query("UPDATE bookmarks SET visitCount = visitCount + 1, lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun incrementVisitCount(id: Long, openedAt: Long)

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>
}
