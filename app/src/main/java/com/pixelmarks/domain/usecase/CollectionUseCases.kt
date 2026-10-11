package com.pixelmarks.domain.usecase

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed interface CreateCollectionResult {
    data class Success(val collection: Collection) : CreateCollectionResult
    data class InvalidName(val message: String) : CreateCollectionResult
    data class InvalidHierarchy(val message: String) : CreateCollectionResult
    data class DuplicateName(val message: String) : CreateCollectionResult
    data class Error(val message: String) : CreateCollectionResult
}

sealed interface UpdateCollectionResult {
    data class Success(val collection: Collection) : UpdateCollectionResult
    data class InvalidName(val message: String) : UpdateCollectionResult
    data class InvalidHierarchy(val message: String) : UpdateCollectionResult
    data class DuplicateName(val message: String) : UpdateCollectionResult
    data class Error(val message: String) : UpdateCollectionResult
}

/**
 * Use case to observe all collections in deterministic position/name order.
 */
/**
 * Use case to observe all collections in deterministic position/name order.
 */
class ObserveCollectionsUseCase(
    private val collectionRepository: CollectionRepository
) {
    operator fun invoke(includeHidden: Boolean = false, isPrivate: Boolean = false): Flow<List<Collection>> = collectionRepository.observeAll(isPrivate)
    fun observeRoots(isPrivate: Boolean = false): Flow<List<Collection>> = collectionRepository.observeRootCollections(isPrivate)
    fun observeChildren(parentId: Long, isPrivate: Boolean = false): Flow<List<Collection>> = collectionRepository.observeChildren(parentId, isPrivate)
}

/**
 * Use case to retrieve a single collection by ID.
 */
class GetCollectionUseCase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(id: Long): Collection? = collectionRepository.getById(id)
}

/**
 * Use case to create a collection with validation and duplicate prevention.
 */
class CreateCollectionUseCase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(
        name: String,
        parentId: Long? = null,
        icon: String? = null,
        color: String? = null,
        isLocked: Boolean = false,
        isHidden: Boolean = false,
        isPrivate: Boolean = false,
        description: String? = null,
        keywords: String? = null
    ): CreateCollectionResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return CreateCollectionResult.InvalidName("Collection name cannot be empty")
        }

        return try {
            val parentCol = if (parentId != null) collectionRepository.getById(parentId) else null
            if (parentCol != null && parentCol.isPrivate != isPrivate) {
                return CreateCollectionResult.InvalidHierarchy(
                    "Collection must have the same privacy level as its parent collection."
                )
            }

            val siblings = if (parentId == null) {
                collectionRepository.observeRootCollections(isPrivate = isPrivate).first()
            } else {
                collectionRepository.observeChildren(parentId, isPrivate = isPrivate).first()
            }

            if (siblings.any { it.name.equals(trimmed, ignoreCase = true) }) {
                return CreateCollectionResult.DuplicateName(
                    "A collection named '$trimmed' already exists at this level"
                )
            }

            val resolvedHidden = isHidden || (parentCol?.isHidden == true)
            val resolvedLocked = isLocked || resolvedHidden

            val now = System.currentTimeMillis()
            val newCollection = Collection(
                name = trimmed,
                parentId = parentId,
                icon = icon,
                color = color,
                position = siblings.size,
                isLocked = resolvedLocked,
                isHidden = resolvedHidden,
                isPrivate = isPrivate,
                description = description?.trim()?.ifEmpty { null },
                keywords = keywords?.trim()?.ifEmpty { null },
                createdAt = now,
                updatedAt = now
            )

            val generatedId = collectionRepository.insert(newCollection)
            CreateCollectionResult.Success(newCollection.copy(id = generatedId))
        } catch (e: Exception) {
            CreateCollectionResult.Error(e.message ?: "Failed to create collection")
        }
    }
}

/**
 * Use case to edit an existing collection, preventing self-parenting and ancestor cycles.
 */
class UpdateCollectionUseCase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(
        collectionId: Long,
        newName: String,
        newParentId: Long? = null,
        icon: String? = null,
        color: String? = null,
        isLocked: Boolean? = null,
        isHidden: Boolean? = null,
        isPrivate: Boolean? = null,
        description: String? = null,
        keywords: String? = null
    ): UpdateCollectionResult {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return UpdateCollectionResult.InvalidName("Collection name cannot be empty")
        }

        val existing = collectionRepository.getById(collectionId)
            ?: return UpdateCollectionResult.Error("Collection not found")

        val resolvedPrivate = isPrivate ?: existing.isPrivate

        // Hierarchy validation
        if (newParentId != null) {
            if (newParentId == collectionId) {
                return UpdateCollectionResult.InvalidHierarchy("A collection cannot be its own parent")
            }

            val parentCol = collectionRepository.getById(newParentId)
            if (parentCol != null && parentCol.isPrivate != resolvedPrivate) {
                return UpdateCollectionResult.InvalidHierarchy(
                    "Collection must have the same privacy level as its parent collection."
                )
            }

            // Cycle prevention: walk up ancestors of newParentId
            var currentAncestorId: Long? = newParentId
            val visited = mutableSetOf<Long>()
            while (currentAncestorId != null) {
                if (currentAncestorId == collectionId) {
                    return UpdateCollectionResult.InvalidHierarchy(
                        "Cannot move a collection into one of its own sub-collections"
                    )
                }
                if (!visited.add(currentAncestorId)) {
                    break // Prevent infinite loop in corrupt data
                }
                val ancestor = collectionRepository.getById(currentAncestorId)
                currentAncestorId = ancestor?.parentId
            }
        }

        // Duplicate sibling name validation (excluding itself)
        val siblings = if (newParentId == null) {
            collectionRepository.observeRootCollections(isPrivate = resolvedPrivate).first()
        } else {
            collectionRepository.observeChildren(newParentId, isPrivate = resolvedPrivate).first()
        }

        if (siblings.any { it.id != collectionId && it.name.equals(trimmed, ignoreCase = true) }) {
            return UpdateCollectionResult.DuplicateName(
                "A collection named '$trimmed' already exists at this level"
            )
        }

        val resolvedHidden = isHidden ?: existing.isHidden
        val resolvedLocked = if (resolvedHidden) true else (isLocked ?: existing.isLocked)

        val updated = existing.copy(
            name = trimmed,
            parentId = newParentId,
            icon = icon ?: existing.icon,
            color = color ?: existing.color,
            isLocked = resolvedLocked,
            isHidden = resolvedHidden,
            isPrivate = resolvedPrivate,
            description = description?.trim()?.ifEmpty { null } ?: existing.description,
            keywords = keywords?.trim()?.ifEmpty { null } ?: existing.keywords,
            updatedAt = System.currentTimeMillis()
        )

        return try {
            collectionRepository.update(updated)
            UpdateCollectionResult.Success(updated)
        } catch (e: Exception) {
            UpdateCollectionResult.Error(e.message ?: "Failed to update collection")
        }
    }
}

/**
 * Use case to delete a collection safely while guaranteeing bookmarks remain intact.
 */
class DeleteCollectionUseCase(
    private val collectionRepository: CollectionRepository,
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(collectionId: Long): Boolean {
        return try {
            // Find all collections in the subtree to explicitly detach bookmarks
            val allCollections = collectionRepository.observeAll().first()
            val targetIds = mutableSetOf(collectionId)

            var addedNew: Boolean
            do {
                addedNew = false
                for (col in allCollections) {
                    if (col.parentId != null && targetIds.contains(col.parentId) && !targetIds.contains(col.id)) {
                        targetIds.add(col.id)
                        addedNew = true
                    }
                }
            } while (addedNew)

            val targetCol = allCollections.firstOrNull { it.id == collectionId }
            val isPrivate = targetCol?.isPrivate ?: false

            // Safely detach all bookmarks referencing this collection subtree
            for (id in targetIds) {
                val bookmarksInCollection = bookmarkRepository.observeByCollection(id, isPrivate = isPrivate).first()
                for (bm in bookmarksInCollection) {
                    bookmarkRepository.update(
                        bm.copy(
                            collectionId = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Delete the collection from Room (cascades to child collections)
            collectionRepository.deleteById(collectionId)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Use case to move or detach a bookmark from a collection without affecting other fields.
 */
class MoveBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository,
    private val collectionRepository: CollectionRepository? = null
) {
    suspend operator fun invoke(bookmarkId: Long, targetCollectionId: Long?): Boolean {
        val bookmark = bookmarkRepository.getById(bookmarkId) ?: return false
        if (bookmark.collectionId == targetCollectionId) return true

        if (targetCollectionId != null && collectionRepository != null) {
            val targetCol = collectionRepository.getById(targetCollectionId) ?: return false
            if (targetCol.isPrivate != bookmark.isPrivate) {
                return false
            }
        }

        val updated = bookmark.copy(
            collectionId = targetCollectionId,
            updatedAt = System.currentTimeMillis()
        )
        return try {
            bookmarkRepository.update(updated)
            true
        } catch (e: Exception) {
            false
        }
    }
}
