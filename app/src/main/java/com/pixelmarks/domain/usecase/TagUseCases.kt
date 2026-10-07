package com.pixelmarks.domain.usecase

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Tag
import com.pixelmarks.data.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed interface CreateTagResult {
    data class Success(val tag: Tag) : CreateTagResult
    data class InvalidName(val message: String) : CreateTagResult
    data class DuplicateName(val message: String) : CreateTagResult
    data class Error(val message: String) : CreateTagResult
}

sealed interface UpdateTagResult {
    data class Success(val tag: Tag) : UpdateTagResult
    data class InvalidName(val message: String) : UpdateTagResult
    data class DuplicateName(val message: String) : UpdateTagResult
    data class Error(val message: String) : UpdateTagResult
}

/**
 * Use case to observe all tags and their reactive bookmark counts.
 */
class ObserveTagsUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(isPrivate: Boolean = false): Flow<List<Tag>> = tagRepository.observeAll(isPrivate)

    fun observeTagCounts(): Flow<Map<Long, Int>> =
        tagRepository.observeAllCrossRefs().map { list ->
            list.groupBy { it.tagId }.mapValues { it.value.size }
        }
}

/**
 * Use case to retrieve a single tag by ID.
 */
class GetTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(id: Long): Tag? = tagRepository.getById(id)
}

/**
 * Use case to create a new tag with validation and case-insensitive duplicate prevention.
 */
class CreateTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(
        name: String,
        color: String? = null,
        isPrivate: Boolean = false
    ): CreateTagResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return CreateTagResult.InvalidName("Tag name cannot be empty")
        }

        return try {
            val allTags = tagRepository.observeAll(isPrivate).first()
            if (allTags.any { it.name.equals(trimmed, ignoreCase = true) }) {
                return CreateTagResult.DuplicateName("A tag with this name already exists")
            }

            val newTag = Tag(
                name = trimmed,
                color = color,
                isPrivate = isPrivate,
                createdAt = System.currentTimeMillis()
            )
            val generatedId = tagRepository.insert(newTag)
            CreateTagResult.Success(newTag.copy(id = generatedId))
        } catch (e: Exception) {
            CreateTagResult.Error(e.message ?: "Failed to create tag")
        }
    }
}

/**
 * Use case to update/rename a tag with validation and collision checks.
 */
class UpdateTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tagId: Long, newName: String, color: String? = null): UpdateTagResult {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return UpdateTagResult.InvalidName("Tag name cannot be empty")
        }

        val existing = tagRepository.getById(tagId)
            ?: return UpdateTagResult.Error("Tag not found")

        val allTags = tagRepository.observeAll(existing.isPrivate).first()
        if (allTags.any { it.id != tagId && it.name.equals(trimmed, ignoreCase = true) }) {
            return UpdateTagResult.DuplicateName("A tag with this name already exists")
        }

        val updated = existing.copy(
            name = trimmed,
            color = color ?: existing.color
        )

        return try {
            tagRepository.update(updated)
            UpdateTagResult.Success(updated)
        } catch (e: Exception) {
            UpdateTagResult.Error(e.message ?: "Failed to update tag")
        }
    }
}

/**
 * Use case to delete a tag safely without affecting bookmarks.
 */
class DeleteTagUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(tagId: Long): Boolean {
        return try {
            tagRepository.deleteById(tagId)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Use case to observe tags assigned to a specific bookmark.
 */
class ObserveBookmarkTagsUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(bookmarkId: Long): Flow<List<Tag>> =
        tagRepository.observeTagsForBookmark(bookmarkId)
}

/**
 * Use case to observe bookmarks that have a specific tag.
 */
class ObserveBookmarksByTagUseCase(
    private val tagRepository: TagRepository
) {
    operator fun invoke(tagId: Long, isPrivate: Boolean = false): Flow<List<Bookmark>> =
        tagRepository.observeBookmarksForTag(tagId, isPrivate)
}

/**
 * Use case to transactionally replace all tags assigned to a bookmark.
 */
class ReplaceBookmarkTagsUseCase(
    private val tagRepository: TagRepository
) {
    suspend operator fun invoke(bookmarkId: Long, tagIds: Set<Long>): Boolean {
        return try {
            tagRepository.setTagsForBookmark(bookmarkId, tagIds)
            true
        } catch (e: Exception) {
            false
        }
    }
}
