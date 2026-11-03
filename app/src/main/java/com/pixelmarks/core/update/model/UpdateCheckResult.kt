package com.pixelmarks.core.update.model

/**
 * Result of a version comparison / update availability check.
 * Provides distinct, explicit states without silently converting errors into "Up to date".
 */
sealed interface UpdateCheckResult {
    /** Check is in progress. */
    data object Loading : UpdateCheckResult

    /** A newer version is available with a verified downloadable APK asset. */
    data class UpdateAvailable(val release: GitHubRelease) : UpdateCheckResult

    /** The installed app is on the newest available version for the selected channel. */
    data object UpToDate : UpdateCheckResult

    /** Network connection is currently unavailable. */
    data object Offline : UpdateCheckResult

    /** GitHub API rate limit reached (unauthenticated: 60 req/hr). */
    data class RateLimited(val resetTime: String) : UpdateCheckResult

    /** The repository release endpoint is unavailable or returned 404. */
    data class ReleaseUnavailable(val message: String) : UpdateCheckResult

    /** A newer release exists, but does not contain a compatible downloadable APK asset. */
    data class ApkAssetMissing(val release: GitHubRelease) : UpdateCheckResult

    /** Release response contained unparseable or corrupted metadata. */
    data class InvalidMetadata(val message: String) : UpdateCheckResult

    /** No published releases found for the specified channel. */
    data class NoReleasesFound(val channel: ReleaseChannel) : UpdateCheckResult

    /** Generic check failure. */
    data class Error(val message: String) : UpdateCheckResult
}
