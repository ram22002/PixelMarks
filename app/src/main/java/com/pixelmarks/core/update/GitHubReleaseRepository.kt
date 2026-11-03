package com.pixelmarks.core.update

import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel

/**
 * Repository for fetching PixelMarks release information from GitHub.
 */
interface GitHubReleaseRepository {
    /**
     * Fetch all published (non-draft) releases in descending date order.
     * Results are cached for [CACHE_DURATION_MS] to respect API rate limits.
     */
    suspend fun getReleases(): Result<List<GitHubRelease>>

    /**
     * Fetch releases filtered to a specific channel.
     * STABLE channel never returns beta/nightly entries.
     */
    suspend fun getReleasesForChannel(channel: ReleaseChannel): Result<List<GitHubRelease>>

    /** Force-expire the in-memory cache and re-fetch on next call. */
    fun invalidateCache()

    companion object {
        const val CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour
    }
}

