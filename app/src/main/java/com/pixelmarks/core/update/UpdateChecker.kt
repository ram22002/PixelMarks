package com.pixelmarks.core.update

import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.UpdateCheckResult

/**
 * Compares the installed app version against available GitHub releases,
 * respecting the user's selected release channel.
 *
 * Version comparison uses semantic versioning (major.minor.patch integers).
 * Falls back to simple string comparison when semver parsing fails.
 */
class UpdateChecker(
    private val releaseRepository: GitHubReleaseRepository,
    private val installedVersionName: String,
    private val installedVersionCode: Int
) {

    /**
     * Check whether a newer release is available for [channel].
     *
     * @param channel The user's selected update channel.
     * @param dismissedVersionName The version the user previously dismissed (skip it).
     */
    suspend fun checkForUpdate(
        channel: ReleaseChannel,
        dismissedVersionName: String? = null
    ): UpdateCheckResult {
        return try {
            val result = releaseRepository.getReleasesForChannel(channel)
            result.fold(
                onSuccess = { releases ->
                    val latest = releases
                        .filter { !it.isDraft }
                        .filter { it.apkAsset != null } // Must have a downloadable APK
                        .maxByOrNull { parseSemVer(it.versionName) }

                    when {
                        latest == null -> UpdateCheckResult.UpToDate
                        latest.versionName == dismissedVersionName -> UpdateCheckResult.UpToDate
                        isNewer(latest.versionName, installedVersionName) -> {
                            UpdateCheckResult.UpdateAvailable(latest)
                        }
                        else -> UpdateCheckResult.UpToDate
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: "Unknown error"
                    if (msg.contains("internet", ignoreCase = true) ||
                        msg.contains("UnknownHost", ignoreCase = true) ||
                        msg.contains("connect", ignoreCase = true)
                    ) {
                        UpdateCheckResult.Offline
                    } else {
                        UpdateCheckResult.Error(msg)
                    }
                }
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Check failed")
        }
    }

    /**
     * Returns true if [candidate] version is strictly newer than [installed].
     */
    internal fun isNewer(candidate: String, installed: String): Boolean {
        val c = parseSemVer(candidate)
        val i = parseSemVer(installed)
        return c > i
    }

    /**
     * Parse "major.minor.patch" into a comparable long: major*1_000_000 + minor*1_000 + patch.
     * Returns 0 for unparseable strings.
     */
    internal fun parseSemVer(version: String): Long {
        return try {
            val parts = version.trimStart('v').substringBefore('-').split(".")
            val major = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val minor = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            val patch = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            major * 1_000_000L + minor * 1_000L + patch
        } catch (_: Exception) {
            0L
        }
    }
}

