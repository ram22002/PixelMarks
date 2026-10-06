package com.pixelmarks.data.repository

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Tag domain operations.
 */
interface TagRepository {
    fun observeAll(): Flow<List<Tag>>
    suspend fun getById(id: Long): Tag?
    suspend fun getByName(name: String): Tag?
    suspend fun insert(tag: Tag): Long
    suspend fun update(tag: Tag)
    suspend fun delete(tag: Tag)
    suspend fun deleteById(id: Long)
    suspend fun addTagToBookmark(bookmarkId: Long, tagId: Long)
    suspend fun removeTagFromBookmark(bookmarkId: Long, tagId: Long)
    suspend fun setTagsForBookmark(bookmarkId: Long, tagIds: Set<Long>)
    fun observeTagsForBookmark(bookmarkId: Long): Flow<List<Tag>>
    fun observeBookmarksForTag(tagId: Long): Flow<List<Bookmark>>
    fun observeAllCrossRefs(): Flow<List<com.pixelmarks.core.database.entity.BookmarkTagCrossRef>>
}
