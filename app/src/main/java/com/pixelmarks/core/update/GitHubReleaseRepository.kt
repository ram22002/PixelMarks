package com.pixelmarks.core.update

import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel

/**
 * Access layer for public GitHub release metadata from the ram22002/PixelMarks repository.
 */
interface GitHubReleaseRepository {

    companion object {
        /** In-memory cache valid for 15 minutes during normal checks. */
        const val CACHE_DURATION_MS = 15 * 60 * 1000L
    }

    /**
     * Retrieve all non-draft releases from GitHub.
     *
     * @param forceRefresh When true, bypasses the in-memory cache and performs a live network fetch.
     */
    suspend fun getReleases(forceRefresh: Boolean = false): Result<List<GitHubRelease>>

    /**
     * Retrieve releases filtered for a specific [channel].
     *
     * @param channel The target distribution channel (Stable, Beta, Nightly).
     * @param forceRefresh When true, forces a network check.
     */
    suspend fun getReleasesForChannel(channel: ReleaseChannel, forceRefresh: Boolean = false): Result<List<GitHubRelease>>

    /**
     * Clear the in-memory release cache so subsequent calls fetch fresh data.
     */
    fun invalidateCache()

    /**
     * Return currently cached releases without network access, if available.
     */
    fun getCachedReleases(): List<GitHubRelease>?
}
