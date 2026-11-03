package com.pixelmarks.core.update.model

/** Result of a version comparison / update availability check. */
sealed interface UpdateCheckResult {
    /** Update available — provide the release to show. */
    data class UpdateAvailable(val release: GitHubRelease) : UpdateCheckResult

    /** App is up to date. */
    data object UpToDate : UpdateCheckResult

    /** Network unavailable at check time. */
    data object Offline : UpdateCheckResult

    /** API returned an error (rate-limit, server error, parse failure). */
    data class Error(val message: String) : UpdateCheckResult

    /** Check is in progress. */
    data object Loading : UpdateCheckResult
}

