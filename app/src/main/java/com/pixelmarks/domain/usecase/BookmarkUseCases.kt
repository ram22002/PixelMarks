package com.pixelmarks.domain.usecase

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.data.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the stream of all bookmarks ordered by pinned and date.
 */
class ObserveBookmarksUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(): Flow<List<Bookmark>> = bookmarkRepository.observeAll()
}

/**
 * Use case to get a bookmark by ID.
 */
class GetBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long): Bookmark? = bookmarkRepository.getById(id)
}

/**
 * Use case to delete a bookmark.
 */
class DeleteBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long) = bookmarkRepository.deleteById(id)
    suspend operator fun invoke(bookmark: Bookmark) = bookmarkRepository.delete(bookmark)
}

/**
 * Use case to update an existing bookmark.
 */
class UpdateBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookmark: Bookmark) = bookmarkRepository.update(bookmark)
}
