package com.pixelmarks.domain.usecase

import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.ReminderRepository
import com.pixelmarks.data.repository.TagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface AddBookmarkResult {
    data class Success(val bookmark: Bookmark) : AddBookmarkResult
    data class Duplicate(val existing: Bookmark) : AddBookmarkResult
    data class InvalidUrl(val message: String) : AddBookmarkResult
    data class Error(val message: String) : AddBookmarkResult
}

/**
 * Use case orchestrating URL validation, normalization, duplicate checking,
 * persistence, and asynchronous metadata enrichment for adding bookmarks.
 */
class AddBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository,
    private val fetchBookmarkMetadataUseCase: FetchBookmarkMetadataUseCase? = null,
    private val tagRepository: TagRepository? = null,
    private val reminderRepository: ReminderRepository? = null,
    private val coroutineScope: CoroutineScope? = null
) {
    suspend operator fun invoke(
        rawUrl: String,
        customTitle: String? = null,
        description: String? = null,
        notes: String? = null,
        thumbnailUrl: String? = null,
        faviconUrl: String? = null,
        reminderAt: Long? = null,
        collectionId: Long? = null,
        tagIds: Set<Long> = emptySet(),
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
                description = description?.trim()?.ifEmpty { null },
                domain = domain,
                faviconUrl = faviconUrl?.trim()?.ifEmpty { null },
                thumbnailUrl = thumbnailUrl?.trim()?.ifEmpty { null },
                collectionId = collectionId,
                isFavorite = false,
                isPinned = false,
                isArchived = false,
                notes = notes?.trim()?.ifEmpty { null },
                createdAt = now,
                updatedAt = now,
                lastOpenedAt = null,
                visitCount = 0,
                reminderAt = reminderAt,
                metadataStatus = MetadataStatus.PENDING,
                metadataUpdatedAt = null
            )

            val generatedId = bookmarkRepository.insert(newBookmark)
            if (tagIds.isNotEmpty()) {
                tagRepository?.setTagsForBookmark(generatedId, tagIds)
            }
            if (reminderAt != null && reminderRepository != null) {
                reminderRepository.setReminder(generatedId, reminderAt)
            }
            val savedBookmark = newBookmark.copy(id = generatedId)

            // Trigger asynchronous metadata enrichment without blocking or gating the save result
            coroutineScope?.launch {
                try {
                    fetchBookmarkMetadataUseCase?.invoke(generatedId)
                } catch (ignored: Exception) {
                    // Metadata extraction is an enrichment step; never fail or cancel save
                }
            }

            AddBookmarkResult.Success(savedBookmark)
        } catch (e: Exception) {
            AddBookmarkResult.Error(e.message ?: "Failed to save bookmark")
        }
    }
}
