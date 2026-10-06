package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.BookmarkTagDao
import com.pixelmarks.core.database.dao.TagDao
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Tag
import com.pixelmarks.data.mapper.toDomain
import com.pixelmarks.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of TagRepository backed by Room TagDao and BookmarkTagDao.
 */
class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val bookmarkTagDao: BookmarkTagDao
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Tag? =
        tagDao.getById(id)?.toDomain()

    override suspend fun getByName(name: String): Tag? =
        tagDao.getByName(name)?.toDomain()

    override suspend fun insert(tag: Tag): Long =
        tagDao.insert(tag.toEntity())

    override suspend fun update(tag: Tag) =
        tagDao.update(tag.toEntity())

    override suspend fun delete(tag: Tag) =
        tagDao.delete(tag.toEntity())

    override suspend fun deleteById(id: Long) =
        tagDao.deleteById(id)

    override suspend fun addTagToBookmark(bookmarkId: Long, tagId: Long) =
        bookmarkTagDao.insert(BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagId))

    override suspend fun removeTagFromBookmark(bookmarkId: Long, tagId: Long) =
        bookmarkTagDao.delete(BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagId))

    override fun observeTagsForBookmark(bookmarkId: Long): Flow<List<Tag>> =
        bookmarkTagDao.observeTagsForBookmark(bookmarkId).map { list -> list.map { it.toDomain() } }

    override fun observeBookmarksForTag(tagId: Long): Flow<List<Bookmark>> =
        bookmarkTagDao.observeBookmarksForTag(tagId).map { list -> list.map { it.toDomain() } }
}
