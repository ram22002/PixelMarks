package com.pixelmarks.core.model

/**
 * Result representation for metadata fetching operations.
 */
sealed interface MetadataFetchResult {
    data class Success(val metadata: PageMetadata) : MetadataFetchResult
    data object NoMetadata : MetadataFetchResult
    data class Blocked(val reason: String) : MetadataFetchResult
    data class Failed(val error: String, val statusCode: Int? = null) : MetadataFetchResult
}
