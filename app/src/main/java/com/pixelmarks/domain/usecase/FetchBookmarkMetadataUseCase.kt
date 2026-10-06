package com.pixelmarks.domain.usecase

import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.MetadataRepository

/**
 * Use case responsible for asynchronously enriching an existing bookmark with website metadata.
 *
 * Guarantees:
 * 1. Safe status transitions (PENDING -> FETCHING -> SUCCESS/NO_METADATA/BLOCKED/FAILED).
 * 2. Never deletes or mutates existing bookmarks destructively on failure.
 * 3. Preserves user-provided titles while upgrading fallback titles.
 * 4. Never throws unhandled exceptions that could crash the caller.
 */
class FetchBookmarkMetadataUseCase(
    private val bookmarkRepository: BookmarkRepository,
    private val metadataRepository: MetadataRepository
) {

    suspend operator fun invoke(bookmarkId: Long): MetadataStatus {
        val bookmark = try {
            bookmarkRepository.getById(bookmarkId)
        } catch (e: Exception) {
            null
        } ?: return MetadataStatus.FAILED

        val fetchingBookmark = bookmark.copy(
            metadataStatus = MetadataStatus.FETCHING,
            updatedAt = System.currentTimeMillis()
        )

        try {
            bookmarkRepository.update(fetchingBookmark)
        } catch (e: Exception) {
            // If updating status fails, continue attempt with current bookmark
        }

        return try {
            val result = metadataRepository.fetchMetadata(bookmark.url)
            val now = System.currentTimeMillis()

            val updatedBookmark = when (result) {
                is MetadataFetchResult.Success -> {
                    val meta = result.metadata

                    // Check if title is auto-generated fallback (matches domain, url, or blank)
                    val isFallbackTitle = bookmark.title.isBlank() ||
                        bookmark.title.equals(bookmark.domain, ignoreCase = true) ||
                        bookmark.title == bookmark.url ||
                        bookmark.title == bookmark.normalizedUrl

                    val resolvedTitle = if (isFallbackTitle && !meta.title.isNullOrBlank()) {
                        meta.title.trim()
                    } else {
                        bookmark.title
                    }

                    val resolvedDescription = if (!bookmark.description.isNullOrBlank()) {
                        bookmark.description
                    } else {
                        meta.description?.takeIf { it.isNotBlank() }
                    }

                    val resolvedThumbnail = if (!bookmark.thumbnailUrl.isNullOrBlank()) {
                        bookmark.thumbnailUrl
                    } else {
                        meta.imageUrl?.takeIf { it.isNotBlank() }
                    }

                    val resolvedFavicon = if (!bookmark.faviconUrl.isNullOrBlank()) {
                        bookmark.faviconUrl
                    } else {
                        meta.faviconUrl?.takeIf { it.isNotBlank() }
                    }

                    bookmark.copy(
                        title = resolvedTitle,
                        description = resolvedDescription,
                        thumbnailUrl = resolvedThumbnail,
                        faviconUrl = resolvedFavicon,
                        metadataStatus = MetadataStatus.SUCCESS,
                        metadataUpdatedAt = now,
                        updatedAt = now
                    )
                }

                is MetadataFetchResult.NoMetadata -> {
                    bookmark.copy(
                        metadataStatus = MetadataStatus.NO_METADATA,
                        metadataUpdatedAt = now,
                        updatedAt = now
                    )
                }

                is MetadataFetchResult.Blocked -> {
                    bookmark.copy(
                        metadataStatus = MetadataStatus.BLOCKED,
                        metadataUpdatedAt = now,
                        updatedAt = now
                    )
                }

                is MetadataFetchResult.Failed -> {
                    bookmark.copy(
                        metadataStatus = MetadataStatus.FAILED,
                        metadataUpdatedAt = now,
                        updatedAt = now
                    )
                }
            }

            bookmarkRepository.update(updatedBookmark)
            updatedBookmark.metadataStatus
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            try {
                bookmarkRepository.update(
                    bookmark.copy(
                        metadataStatus = MetadataStatus.FAILED,
                        metadataUpdatedAt = now,
                        updatedAt = now
                    )
                )
            } catch (ignored: Exception) {
                // Safeguard against secondary failure
            }
            MetadataStatus.FAILED
        }
    }
}
