package com.pixelmarks.data.repository

import androidx.room.withTransaction
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.html.NetscapeBookmarkExporter
import com.pixelmarks.core.html.NetscapeBookmarkParser
import com.pixelmarks.core.model.html.HtmlBookmarkItem
import com.pixelmarks.core.model.html.HtmlExportResult
import com.pixelmarks.core.model.html.HtmlExportScope
import com.pixelmarks.core.model.html.HtmlImportCollectionTarget
import com.pixelmarks.core.model.html.HtmlImportDuplicatePolicy
import com.pixelmarks.core.model.html.HtmlImportOptions
import com.pixelmarks.core.model.html.HtmlImportPreview
import com.pixelmarks.core.model.html.HtmlImportSummary
import com.pixelmarks.core.model.html.HtmlParseResult
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.data.mapper.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * Repository interface for Netscape Bookmark HTML import and export.
 */
interface HtmlBookmarkRepository {
    suspend fun parseHtml(inputStream: InputStream): HtmlParseResult
    suspend fun getImportPreview(parseResult: HtmlParseResult, isPrivate: Boolean): HtmlImportPreview
    suspend fun importBookmarks(
        parseResult: HtmlParseResult,
        options: HtmlImportOptions,
        isPrivate: Boolean,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): HtmlImportSummary
    suspend fun exportBookmarks(
        scope: HtmlExportScope,
        isPrivate: Boolean,
        outputStream: OutputStream
    ): HtmlExportResult
}

class HtmlBookmarkRepositoryImpl(
    private val database: PixelMarksDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HtmlBookmarkRepository {

    override suspend fun parseHtml(inputStream: InputStream): HtmlParseResult = withContext(ioDispatcher) {
        NetscapeBookmarkParser.parse(inputStream)
    }

    override suspend fun getImportPreview(
        parseResult: HtmlParseResult,
        isPrivate: Boolean
    ): HtmlImportPreview = withContext(ioDispatcher) {
        var existingDbDuplicateCount = 0

        // Check each valid bookmark against the active privacy boundary in Room
        for (item in parseResult.validBookmarks) {
            coroutineContext.ensureActive()
            val existing = database.bookmarkDao().getByNormalizedUrlAndPrivacy(item.normalizedUrl, isPrivate)
            if (existing.isNotEmpty()) {
                existingDbDuplicateCount++
            }
        }

        HtmlImportPreview(
            totalFound = parseResult.totalFound,
            validCount = parseResult.validBookmarks.size,
            invalidCount = parseResult.invalidEntries.size,
            fileDuplicateCount = parseResult.fileDuplicateCount,
            existingDbDuplicateCount = existingDbDuplicateCount,
            detectedFolders = parseResult.detectedFolders,
            sampleBookmarks = parseResult.validBookmarks.take(5),
            invalidEntries = parseResult.invalidEntries.take(10)
        )
    }

    override suspend fun importBookmarks(
        parseResult: HtmlParseResult,
        options: HtmlImportOptions,
        isPrivate: Boolean,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): HtmlImportSummary = withContext(ioDispatcher) {
        var importedCount = 0
        var skippedDuplicatesCount = 0
        var updatedCount = 0
        var failedCount = 0
        var createdCollectionsCount = 0

        database.withTransaction {
            val now = System.currentTimeMillis()

            // Cache existing collections for the active privacy boundary: (lowercase name, parentId) -> CollectionEntity
            val existingCollections = database.collectionDao().getAllByPrivacy(isPrivate).toMutableList()
            val collectionMap = mutableMapOf<Pair<String, Long?>, CollectionEntity>()
            for (col in existingCollections) {
                collectionMap[col.name.lowercase(Locale.ROOT) to col.parentId] = col
            }

            // Cache existing tags for the active privacy boundary: lowercase name -> TagEntity
            val existingTags = database.tagDao().getAllByPrivacy(isPrivate).toMutableList()
            val tagMap = mutableMapOf<String, TagEntity>()
            for (tag in existingTags) {
                tagMap[tag.name.lowercase(Locale.ROOT)] = tag
            }

            // Helper to resolve or create nested folder hierarchy as collections
            suspend fun resolveCollectionIdForPath(path: List<String>): Long? {
                if (path.isEmpty()) return null
                var parentId: Long? = null

                for (folder in path) {
                    val cleanFolder = folder.trim().take(100)
                    if (cleanFolder.isEmpty()) continue

                    val key = cleanFolder.lowercase(Locale.ROOT) to parentId
                    var col = collectionMap[key]

                    if (col == null) {
                        val newCol = CollectionEntity(
                            name = cleanFolder,
                            parentId = parentId,
                            isPrivate = isPrivate,
                            createdAt = now,
                            updatedAt = now
                        )
                        val id = database.collectionDao().insert(newCol)
                        col = newCol.copy(id = id)
                        collectionMap[key] = col
                        createdCollectionsCount++
                    }
                    parentId = col.id
                }
                return parentId
            }

            // Determine target collection ID strategy
            val fixedCollectionId = when (val target = options.collectionTarget) {
                is HtmlImportCollectionTarget.RootOnly -> null
                is HtmlImportCollectionTarget.ExistingCollection -> {
                    // Verify collection exists in current privacy boundary
                    val col = database.collectionDao().getById(target.collectionId)
                    if (col != null && col.isPrivate == isPrivate) col.id else null
                }
                is HtmlImportCollectionTarget.PreserveFolders -> null // Determined per-item
            }

            val totalItems = parseResult.validBookmarks.size

            for ((index, item) in parseResult.validBookmarks.withIndex() ) {
                coroutineContext.ensureActive()

                try {
                    val resolvedCollectionId = if (options.collectionTarget is HtmlImportCollectionTarget.PreserveFolders) {
                        resolveCollectionIdForPath(item.folderPath)
                    } else {
                        fixedCollectionId
                    }

                    // Check for existing duplicates in the target privacy boundary
                    val existingList = database.bookmarkDao().getByNormalizedUrlAndPrivacy(item.normalizedUrl, isPrivate)

                    if (existingList.isNotEmpty()) {
                        when (options.duplicatePolicy) {
                            HtmlImportDuplicatePolicy.SKIP -> {
                                skippedDuplicatesCount++
                                onProgress?.invoke(index + 1, totalItems)
                                continue
                            }

                            HtmlImportDuplicatePolicy.UPDATE -> {
                                val existing = existingList.first()
                                val updatedEntity = existing.copy(
                                    title = item.title.ifBlank { existing.title },
                                    description = item.description ?: existing.description,
                                    collectionId = resolvedCollectionId ?: existing.collectionId,
                                    updatedAt = now
                                )
                                database.bookmarkDao().update(updatedEntity)
                                attachTagsToBookmark(existing.id, item.tags, isPrivate, tagMap, now)
                                updatedCount++
                                onProgress?.invoke(index + 1, totalItems)
                                continue
                            }

                            HtmlImportDuplicatePolicy.IMPORT_NEW -> {
                                // Proceed to insert as a new bookmark below
                            }
                        }
                    }

                    // Insert new bookmark
                    val newBookmark = BookmarkEntity(
                        url = item.url,
                        normalizedUrl = item.normalizedUrl,
                        title = item.title.ifBlank { UrlUtils.extractDomain(item.url).ifEmpty { item.url } },
                        domain = UrlUtils.extractDomain(item.url),
                        collectionId = resolvedCollectionId,
                        description = item.description,
                        isPrivate = isPrivate,
                        createdAt = item.addedDate ?: now,
                        updatedAt = item.modifiedDate ?: now,
                        metadataStatus = MetadataStatus.PENDING
                    )
                    val insertedId = database.bookmarkDao().insert(newBookmark)
                    attachTagsToBookmark(insertedId, item.tags, isPrivate, tagMap, now)
                    importedCount++

                } catch (e: Exception) {
                    coroutineContext.ensureActive()
                    failedCount++
                }

                onProgress?.invoke(index + 1, totalItems)
            }
        }

        HtmlImportSummary(
            importedCount = importedCount,
            skippedDuplicatesCount = skippedDuplicatesCount,
            updatedCount = updatedCount,
            invalidCount = parseResult.invalidEntries.size,
            failedCount = failedCount,
            createdCollectionsCount = createdCollectionsCount
        )
    }

    private suspend fun attachTagsToBookmark(
        bookmarkId: Long,
        tags: List<String>,
        isPrivate: Boolean,
        tagMap: MutableMap<String, TagEntity>,
        now: Long
    ) {
        for (tagName in tags) {
            val cleanTag = tagName.trim().take(50)
            if (cleanTag.isEmpty()) continue

            val key = cleanTag.lowercase(Locale.ROOT)
            var tagEntity = tagMap[key]

            if (tagEntity == null) {
                val newTag = TagEntity(
                    name = cleanTag,
                    isPrivate = isPrivate,
                    createdAt = now
                )
                val newId = database.tagDao().insert(newTag)
                tagEntity = newTag.copy(id = newId)
                tagMap[key] = tagEntity
            }

            database.bookmarkTagDao().insert(
                BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = tagEntity.id)
            )
        }
    }

    override suspend fun exportBookmarks(
        scope: HtmlExportScope,
        isPrivate: Boolean,
        outputStream: OutputStream
    ): HtmlExportResult = withContext(ioDispatcher) {
        val allCollections = database.collectionDao().getAllByPrivacy(isPrivate).map { it.toDomain() }

        val bookmarksToExport: List<com.pixelmarks.core.model.Bookmark> = when (scope) {
            is HtmlExportScope.All -> {
                database.bookmarkDao().getAllByPrivacy(isPrivate).map { it.toDomain() }
            }

            is HtmlExportScope.CollectionSubtree -> {
                // Collect subtree collection IDs
                val subtreeIds = mutableSetOf(scope.collectionId)
                var expanded = true
                while (expanded) {
                    expanded = false
                    for (col in allCollections) {
                        if (col.parentId != null && subtreeIds.contains(col.parentId) && !subtreeIds.contains(col.id)) {
                            subtreeIds.add(col.id)
                            expanded = true
                        }
                    }
                }

                database.bookmarkDao().getAllByPrivacy(isPrivate)
                    .filter { it.collectionId != null && subtreeIds.contains(it.collectionId) }
                    .map { it.toDomain() }
            }
        }

        // Fetch tags for exported bookmarks
        val tagsMap = mutableMapOf<Long, List<String>>()
        for (b in bookmarksToExport) {
            val tags = database.bookmarkTagDao().getTagsForBookmark(b.id).map { it.name }
            if (tags.isNotEmpty()) {
                tagsMap[b.id] = tags
            }
        }

        NetscapeBookmarkExporter.export(
            bookmarks = bookmarksToExport,
            collections = allCollections,
            tagsMap = tagsMap,
            scope = scope,
            isPrivate = isPrivate,
            outputStream = outputStream
        )
    }
}
