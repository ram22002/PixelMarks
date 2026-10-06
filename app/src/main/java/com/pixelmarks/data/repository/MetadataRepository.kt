package com.pixelmarks.data.repository

import com.pixelmarks.core.model.MetadataFetchResult

/**
 * Repository for fetching and extracting website metadata.
 */
interface MetadataRepository {
    suspend fun fetchMetadata(url: String): MetadataFetchResult
}
