package com.pixelmarks.core.model.html

/**
 * Parsed bookmark item from a Netscape Bookmark HTML file.
 */
data class HtmlBookmarkItem(
    val url: String,
    val normalizedUrl: String,
    val title: String,
    val folderPath: List<String> = emptyList(),
    val addedDate: Long? = null,
    val modifiedDate: Long? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null
)

/**
 * Details of an entry in the HTML file that could not be parsed as a valid bookmark.
 */
data class HtmlInvalidEntry(
    val rawUrl: String?,
    val title: String?,
    val reason: String
)

/**
 * Result of parsing a Netscape Bookmark HTML stream.
 */
data class HtmlParseResult(
    val totalFound: Int,
    val validBookmarks: List<HtmlBookmarkItem>,
    val invalidEntries: List<HtmlInvalidEntry>,
    val detectedFolders: List<String>,
    val fileDuplicateCount: Int
)

/**
 * Duplicate handling policy for bookmark import.
 */
enum class HtmlImportDuplicatePolicy {
    /**
     * Skip entries whose normalized URL already exists in the target privacy boundary.
     */
    SKIP,

    /**
     * Import the entry as a new bookmark regardless of existing entries.
     */
    IMPORT_NEW,

    /**
     * Update title, description, and tags on the existing bookmark.
     */
    UPDATE
}

/**
 * Collection destination option when importing bookmarks.
 */
sealed interface HtmlImportCollectionTarget {
    /**
     * Reconstruct the browser folder hierarchy as nested collections.
     */
    data object PreserveFolders : HtmlImportCollectionTarget

    /**
     * Place all imported bookmarks in root (no collection).
     */
    data object RootOnly : HtmlImportCollectionTarget

    /**
     * Place all imported bookmarks into an existing collection.
     */
    data class ExistingCollection(val collectionId: Long) : HtmlImportCollectionTarget
}

/**
 * User options configured before committing an import.
 */
data class HtmlImportOptions(
    val duplicatePolicy: HtmlImportDuplicatePolicy = HtmlImportDuplicatePolicy.SKIP,
    val collectionTarget: HtmlImportCollectionTarget = HtmlImportCollectionTarget.PreserveFolders
)

/**
 * Pre-import summary presented to the user.
 */
data class HtmlImportPreview(
    val totalFound: Int,
    val validCount: Int,
    val invalidCount: Int,
    val fileDuplicateCount: Int,
    val existingDbDuplicateCount: Int,
    val detectedFolders: List<String>,
    val sampleBookmarks: List<HtmlBookmarkItem>,
    val invalidEntries: List<HtmlInvalidEntry>
)

/**
 * Post-import execution summary.
 */
data class HtmlImportSummary(
    val importedCount: Int,
    val skippedDuplicatesCount: Int,
    val updatedCount: Int,
    val invalidCount: Int,
    val failedCount: Int,
    val createdCollectionsCount: Int
)

/**
 * Scope options for bookmark export.
 */
sealed interface HtmlExportScope {
    data object All : HtmlExportScope
    data class CollectionSubtree(val collectionId: Long, val collectionName: String) : HtmlExportScope
}

/**
 * Outcome of an HTML export operation.
 */
data class HtmlExportResult(
    val exportedBookmarksCount: Int,
    val exportedCollectionsCount: Int,
    val bytesWritten: Long,
    val isPrivate: Boolean
)
