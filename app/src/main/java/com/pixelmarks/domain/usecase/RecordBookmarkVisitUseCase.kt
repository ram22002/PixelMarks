package com.pixelmarks.domain.usecase

import com.pixelmarks.data.repository.BookmarkRepository

/**
 * Use case to atomically update the bookmark's visitCount (+1) and lastOpenedAt timestamp.
 */
class RecordBookmarkVisitUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookmarkId: Long, openedAt: Long = System.currentTimeMillis()) {
        bookmarkRepository.recordVisit(bookmarkId, openedAt)
    }
}
