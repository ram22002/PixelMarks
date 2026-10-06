package com.pixelmarks.domain.usecase

import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.data.repository.BookmarkRepository

sealed interface AddBookmarkResult {
    data class Success(val bookmark: Bookmark) : AddBookmarkResult
    data class Duplicate(val existing: Bookmark) : AddBookmarkResult
    data class InvalidUrl(val message: String) : AddBookmarkResult
    data class Error(val message: String) : AddBookmarkResult
}

/**
 * Use case orchestrating URL validation, normalization, duplicate checking,
 * and persistence for adding bookmarks.
 */
class AddBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(
        rawUrl: String,
        customTitle: String? = null,
        collectionId: Long? = null,
        forceSave: Boolean = false
    ): AddBookmarkResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return AddBookmarkResult.InvalidUrl("URL cannot be empty")
        }

        if (!UrlUtils.isValidUrl(trimmed)) {
            return AddBookmarkResult.InvalidUrl("Please enter a valid web URL (e.g. example.com)")
        }

        return try {
            val normalizedUrl = UrlUtils.normalizeUrl(trimmed)
            val domain = UrlUtils.extractDomain(normalizedUrl)

            if (!forceSave) {
                val existing = bookmarkRepository.findByNormalizedUrl(normalizedUrl)
                if (existing.isNotEmpty()) {
                    return AddBookmarkResult.Duplicate(existing.first())
                }
            }

            val now = System.currentTimeMillis()
            val title = if (!customTitle.isNullOrBlank()) {
                customTitle.trim()
            } else {
                domain.ifBlank { normalizedUrl }
            }

            val newBookmark = Bookmark(
                url = normalizedUrl,
                normalizedUrl = normalizedUrl,
                title = title,
                description = null,
                domain = domain,
                faviconUrl = null,
                thumbnailUrl = null,
                collectionId = collectionId,
                isFavorite = false,
                isPinned = false,
                isArchived = false,
                notes = null,
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = null,
                visitCount = 0,
                reminderAt = null,
                metadataStatus = MetadataStatus.PENDING,
                metadataUpdatedAt = null
            )

            val generatedId = bookmarkRepository.insert(newBookmark)
            AddBookmarkResult.Success(newBookmark.copy(id = generatedId))
        } catch (e: Exception) {
            AddBookmarkResult.Error(e.message ?: "Failed to save bookmark")
        }
    }
}
