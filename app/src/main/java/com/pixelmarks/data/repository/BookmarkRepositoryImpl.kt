package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.BookmarkDao
import com.pixelmarks.core.database.dao.BookmarkTagDao
import com.pixelmarks.core.database.dao.TagDao
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.core.util.SearchQueryUtils
import com.pixelmarks.data.mapper.toDomain
import com.pixelmarks.data.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Implementation of BookmarkRepository backed by Room BookmarkDao.
 */
class BookmarkRepositoryImpl(
    private val bookmarkDao: BookmarkDao,
    private val tagDao: TagDao? = null,
    private val bookmarkTagDao: BookmarkTagDao? = null
) : BookmarkRepository {

    private fun Flow<List<BookmarkEntity>>.attachTags(isPrivate: Boolean = false): Flow<List<Bookmark>> {
        val tDao = tagDao
        val btDao = bookmarkTagDao
        return if (tDao != null && btDao != null) {
            combine(
                this,
                tDao.observeAll(isPrivate),
                btDao.observeAllCrossRefs()
            ) { bookmarks, tags, refs ->
                val tagsById = tags.associate { it.id to it.toDomain() }
                val tagsByBookmark = refs.groupBy { it.bookmarkId }
                bookmarks.map { b ->
                    val bTags = tagsByBookmark[b.id].orEmpty().mapNotNull { tagsById[it.tagId] }
                    b.toDomain().copy(tags = bTags)
                }
            }
        } else {
            this.map { list -> list.map { it.toDomain() } }
        }
    }

    override fun observeAll(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeAll(isPrivate).attachTags(isPrivate)

    override fun observeByCollection(collectionId: Long, isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeByCollection(collectionId, isPrivate).attachTags(isPrivate)

    override fun observeUnsorted(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeUnsorted(isPrivate).attachTags(isPrivate)

    override fun observeById(id: Long): Flow<Bookmark?> {
        val tDao = tagDao
        val btDao = bookmarkTagDao
        return if (tDao != null && btDao != null) {
            combine(
                bookmarkDao.observeById(id),
                tDao.observeAll(),
                btDao.observeAllCrossRefs()
            ) { bookmark, tags, refs ->
                if (bookmark == null) null
                else {
                    val tagsById = tags.associate { it.id to it.toDomain() }
                    val bTags = refs.filter { it.bookmarkId == id }.mapNotNull { tagsById[it.tagId] }
                    bookmark.toDomain().copy(tags = bTags)
                }
            }
        } else {
            bookmarkDao.observeById(id).map { it?.toDomain() }
        }
    }

    override fun observeFavorites(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeFavorites(isPrivate).attachTags(isPrivate)

    override fun observePinned(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observePinned(isPrivate).attachTags(isPrivate)

    override fun observeArchived(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeArchived(isPrivate).attachTags(isPrivate)

    override fun observeTrashed(isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeTrashed(isPrivate).attachTags(isPrivate)

    override fun observeByDomain(domain: String, isPrivate: Boolean): Flow<List<Bookmark>> =
        bookmarkDao.observeByDomain(domain, isPrivate).attachTags(isPrivate)

    override fun observeTotalCount(isPrivate: Boolean): Flow<Int> =
        bookmarkDao.observeCount(isPrivate)

    override fun observeTrashedCount(isPrivate: Boolean): Flow<Int> =
        bookmarkDao.observeTrashedCount(isPrivate)

    override fun observeBookmarks(
        query: String,
        filters: BookmarkFilters,
        sort: BookmarkSortOption,
        isPrivate: Boolean
    ): Flow<List<Bookmark>> {
        val trimmedQuery = query.trim()
        val escapedQuery = SearchQueryUtils.escapeSqlLike(trimmedQuery)
        val entitiesFlow = bookmarkDao.searchBookmarks(
            query = trimmedQuery,
            escapedQuery = escapedQuery,
            favoriteOnly = filters.favoriteOnly,
            pinnedOnly = filters.pinnedOnly,
            archivedOnly = filters.archivedOnly,
            collectionId = filters.collectionId,
            tagId = filters.tagId,
            isPrivate = isPrivate
        )
        return entitiesFlow.attachTags(isPrivate).map { list ->
            list.sortedWith(sort.comparator)
        }
    }

    override suspend fun getById(id: Long): Bookmark? {
        val entity = bookmarkDao.getById(id) ?: return null
        val btDao = bookmarkTagDao
        val tDao = tagDao
        val tags = if (btDao != null && tDao != null) {
            btDao.getTagsForBookmark(id).map { it.toDomain() }
        } else {
            emptyList()
        }
        return entity.toDomain().copy(tags = tags)
    }

    override suspend fun findByNormalizedUrl(normalizedUrl: String, isPrivate: Boolean): List<Bookmark> =
        bookmarkDao.getByNormalizedUrlAndPrivacy(normalizedUrl, isPrivate).map { it.toDomain() }

    override suspend fun getAllByPrivacy(isPrivate: Boolean): List<Bookmark> =
        bookmarkDao.getAllByPrivacy(isPrivate).map { it.toDomain() }

    override suspend fun insert(bookmark: Bookmark): Long =
        bookmarkDao.insert(bookmark.toEntity())

    override suspend fun update(bookmark: Bookmark) =
        bookmarkDao.update(bookmark.toEntity())

    override suspend fun delete(bookmark: Bookmark) =
        bookmarkDao.delete(bookmark.toEntity())

    override suspend fun deleteById(id: Long) =
        bookmarkDao.deleteById(id)

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        bookmarkDao.setFavorite(id, isFavorite)

    override suspend fun setPinned(id: Long, isPinned: Boolean) =
        bookmarkDao.setPinned(id, isPinned)

    override suspend fun setArchived(id: Long, isArchived: Boolean) =
        bookmarkDao.setArchived(id, isArchived)

    override suspend fun moveToTrash(id: Long) =
        bookmarkDao.moveToTrash(id)

    override suspend fun restoreFromTrash(id: Long) =
        bookmarkDao.restoreFromTrash(id)

    override suspend fun permanentlyDelete(id: Long) =
        bookmarkDao.deleteById(id)

    override suspend fun recordVisit(id: Long, openedAt: Long) =
        bookmarkDao.incrementVisitCount(id, openedAt)
}
