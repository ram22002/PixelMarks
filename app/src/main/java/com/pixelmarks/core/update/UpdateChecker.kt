package com.pixelmarks.core.update

import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.SemVer
import com.pixelmarks.core.update.model.UpdateCheckResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compares the installed app version against available GitHub releases,
 * respecting the user's selected release channel and semantic versioning rules.
 *
 * Implements SemVer 2.0.0 precedence:
 * - Understands prerelease identifiers (1.0.0-beta < 1.0.1-beta < 1.0.1)
 * - Excludes draft releases
 * - Validates APK asset presence
 * - Never offers downgrades
 */
class UpdateChecker(
    private val releaseRepository: GitHubReleaseRepository,
    private val installedVersionName: String,
    private val installedVersionCode: Int
) {

    /**
     * Check whether a newer release is available on GitHub for [channel].
     *
     * @param channel The user's selected update channel.
     * @param dismissedVersionName The version the user previously dismissed (skip showing).
     */
    suspend fun checkForUpdate(
        channel: ReleaseChannel,
        dismissedVersionName: String? = null
    ): UpdateCheckResult {
        return try {
            val result = releaseRepository.getReleasesForChannel(channel)
            result.fold(
                onSuccess = { releases ->
                    val validReleases = releases.filter { !it.isDraft }
                    if (validReleases.isEmpty()) {
                        return UpdateCheckResult.NoReleasesFound(channel)
                    }

                    // Pick the highest version release published in this channel
                    val latest = validReleases.maxByOrNull { it.semVer }
                        ?: return UpdateCheckResult.NoReleasesFound(channel)

                    val installedSemVer = SemVer.parse(installedVersionName)
                    val candidateSemVer = latest.semVer

                    when {
                        // Candidate is not strictly newer (already on latest or installed is newer)
                        candidateSemVer <= installedSemVer -> {
                            UpdateCheckResult.UpToDate
                        }
                        // Newer version exists, but has no compatible APK asset attached
                        latest.apkAsset == null -> {
                            UpdateCheckResult.ApkAssetMissing(latest)
                        }
                        // Valid newer update available
                        else -> {
                            UpdateCheckResult.UpdateAvailable(latest)
                        }
                    }
                },
                onFailure = { e ->
                    mapFailureToResult(e)
                }
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Update check encountered an unexpected error")
        }
    }

    /**
     * Returns true if [candidate] is strictly newer than [installed] under SemVer 2.0.0 rules.
     */
    fun isNewer(candidate: String, installed: String): Boolean {
        val c = SemVer.parse(candidate)
        val i = SemVer.parse(installed)
        return c > i
    }

    private fun mapFailureToResult(e: Throwable): UpdateCheckResult {
        val msg = e.message ?: "Unknown error"
        return when {
            msg.contains("rate limit", ignoreCase = true) -> {
                val resetFormatted = formatRateLimitReset(msg)
                UpdateCheckResult.RateLimited(resetFormatted)
            }
            msg.contains("internet", ignoreCase = true) ||
                msg.contains("UnknownHost", ignoreCase = true) ||
                msg.contains("connect", ignoreCase = true) -> {
                UpdateCheckResult.Offline
            }
            msg.contains("404") || msg.contains("not found", ignoreCase = true) -> {
                UpdateCheckResult.ReleaseUnavailable("GitHub releases endpoint not found")
            }
            msg.contains("JSON", ignoreCase = true) || msg.contains("malformed", ignoreCase = true) -> {
                UpdateCheckResult.InvalidMetadata(msg)
            }
            else -> {
                UpdateCheckResult.Error(msg)
            }
        }
    }

    private fun formatRateLimitReset(message: String): String {
        return try {
            val epochStr = message.substringAfter("Resets at:").trim().takeWhile { it.isDigit() }
            if (epochStr.isNotEmpty()) {
                val epochSeconds = epochStr.toLong()
                val date = Date(epochSeconds * 1000L)
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                sdf.format(date)
            } else {
                "in ~1 hour"
            }
        } catch (_: Exception) {
            "in ~1 hour"
        }
    }
}
