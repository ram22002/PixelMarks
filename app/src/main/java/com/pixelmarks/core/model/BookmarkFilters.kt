package com.pixelmarks.core.model

/**
 * Filter criteria for querying and searching bookmarks.
 * Filters are composable and combined using AND logic.
 */
data class BookmarkFilters(
    val favoriteOnly: Boolean = false,
    val pinnedOnly: Boolean = false,
    val archivedOnly: Boolean = false,
    val collectionId: Long? = null,
    val tagId: Long? = null
) {
    val isActive: Boolean
        get() = favoriteOnly || pinnedOnly || archivedOnly || collectionId != null || tagId != null
}
