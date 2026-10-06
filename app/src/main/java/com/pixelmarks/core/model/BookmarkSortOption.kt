package com.pixelmarks.core.model

import com.pixelmarks.core.model.Bookmark

/**
 * Deterministic sorting options for bookmarks.
 * Uses secondary ordering by id to guarantee stable results.
 */
enum class BookmarkSortOption(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    TITLE("Title"),
    RECENTLY_OPENED("Recently Opened"),
    MOST_VISITED("Most Visited"),
    RECENTLY_MODIFIED("Recently Modified");

    val comparator: Comparator<Bookmark>
        get() = when (this) {
            NEWEST -> Comparator { a, b ->
                val cmp = b.createdAt.compareTo(a.createdAt)
                if (cmp != 0) cmp else b.id.compareTo(a.id)
            }
            OLDEST -> Comparator { a, b ->
                val cmp = a.createdAt.compareTo(b.createdAt)
                if (cmp != 0) cmp else a.id.compareTo(b.id)
            }
            TITLE -> Comparator { a, b ->
                val cmp = a.title.compareTo(b.title, ignoreCase = true)
                if (cmp != 0) cmp else a.id.compareTo(b.id)
            }
            RECENTLY_OPENED -> Comparator { a, b ->
                val timeA = a.lastOpenedAt
                val timeB = b.lastOpenedAt
                when {
                    timeA == null && timeB == null -> b.id.compareTo(a.id)
                    timeA == null -> 1 // nulls last
                    timeB == null -> -1 // non-null first
                    timeA != timeB -> timeB.compareTo(timeA) // descending
                    else -> b.id.compareTo(a.id)
                }
            }
            MOST_VISITED -> Comparator { a, b ->
                val cmp = b.visitCount.compareTo(a.visitCount)
                if (cmp != 0) cmp else b.id.compareTo(a.id)
            }
            RECENTLY_MODIFIED -> Comparator { a, b ->
                val cmp = b.updatedAt.compareTo(a.updatedAt)
                if (cmp != 0) cmp else b.id.compareTo(a.id)
            }
        }
}
