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

/**
 * Use case to toggle or set bookmark favorite state.
 */
class ToggleBookmarkFavoriteUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long, isFavorite: Boolean) {
        bookmarkRepository.setFavorite(id, isFavorite)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.setFavorite(bookmark.id, !bookmark.isFavorite)
    }
}

/**
 * Use case to toggle or set bookmark pinned state.
 */
class ToggleBookmarkPinUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long, isPinned: Boolean) {
        bookmarkRepository.setPinned(id, isPinned)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.setPinned(bookmark.id, !bookmark.isPinned)
    }
}

/**
 * Use case to archive a bookmark.
 */
class ArchiveBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long) {
        bookmarkRepository.setArchived(id, true)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.setArchived(bookmark.id, true)
    }
}

/**
 * Use case to unarchive a bookmark.
 */
class UnarchiveBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long) {
        bookmarkRepository.setArchived(id, false)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.setArchived(bookmark.id, false)
    }
}

/**
 * Use case to move a bookmark to trash.
 */
class TrashBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long) {
        bookmarkRepository.moveToTrash(id)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.moveToTrash(bookmark.id)
    }
}

/**
 * Use case to restore a bookmark from trash.
 */
class RestoreBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long) {
        bookmarkRepository.restoreFromTrash(id)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.restoreFromTrash(bookmark.id)
    }
}

/**
 * Use case to permanently delete a bookmark.
 */
class PermanentlyDeleteBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: Long) {
        bookmarkRepository.permanentlyDelete(id)
    }

    suspend operator fun invoke(bookmark: Bookmark) {
        bookmarkRepository.permanentlyDelete(bookmark.id)
    }
}

/**
 * Use case to observe trashed bookmarks.
 */
class ObserveTrashedBookmarksUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(): Flow<List<Bookmark>> = bookmarkRepository.observeTrashed()
}

/**
 * Use case to observe a single bookmark by ID reactively.
 */
class ObserveBookmarkByIdUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(id: Long): Flow<Bookmark?> = bookmarkRepository.observeById(id)
}

