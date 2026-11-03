package com.pixelmarks.core.update.model

/**
 * A GitHub release record, mapped from the GitHub Releases API response.
 *
 * @param id          GitHub release ID
 * @param tagName     e.g. "v1.0.1-beta" or "v1.0.0"
 * @param name        Release title
 * @param body        Markdown release notes
 * @param publishedAt ISO-8601 timestamp string
 * @param htmlUrl     URL of the release page on GitHub
 * @param assets      Attached release assets
 * @param channel     Inferred release channel
 * @param isDraft     Draft status (drafts are never shown or offered for update)
 * @param isPreRelease Prerelease status
 */
data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>,
    val channel: ReleaseChannel,
    val isDraft: Boolean,
    val isPreRelease: Boolean
) {
    /**
     * The parsed [SemVer] object for this release tag.
     */
    val semVer: SemVer by lazy { SemVer.parse(tagName) }

    /**
     * Clean version name string without leading "v", preserving any prerelease identifier.
     * e.g., "1.0.1-beta" or "1.0.0".
     */
    val versionName: String
        get() = tagName.trim().trimStart('v', 'V')

    /**
     * Human-readable title: uses [name] if provided, falls back to [tagName].
     */
    val displayTitle: String
        get() = name.ifBlank { tagName }

    /**
     * Returns true if this release has a non-empty, meaningful markdown body.
     */
    val hasReleaseNotes: Boolean
        get() = body.isNotBlank() && !body.trim().equals("No release notes provided.", ignoreCase = true)

    /**
     * Select the compatible APK asset attached to this release.
     * Excludes source archives (.zip, .tar.gz), SVGs, checksums, and non-APK assets.
     */
    val apkAsset: ReleaseAsset?
        get() = selectCompatibleApk(assets)

    companion object {
        /**
         * Robust asset filter and selector for Android APK files.
         * Never selects source-code ZIP/TAR.GZ archives, SVGs, or checksum files.
         */
        fun selectCompatibleApk(assets: List<ReleaseAsset>): ReleaseAsset? {
            val apkCandidates = assets.filter { asset ->
                val lower = asset.name.lowercase()
                lower.endsWith(".apk") &&
                    !lower.endsWith(".zip") &&
                    !lower.endsWith(".tar.gz") &&
                    !lower.endsWith(".svg") &&
                    !lower.endsWith(".sha256") &&
                    !lower.endsWith(".md5") &&
                    asset.sizeBytes > 0L
            }

            if (apkCandidates.isEmpty()) return null
            if (apkCandidates.size == 1) return apkCandidates.first()

            // When multiple APKs are attached, pick the most appropriate:
            // 1. Universal build
            // 2. Debug build (for beta/development testing)
            // 3. First available valid APK
            return apkCandidates.firstOrNull { it.name.contains("universal", ignoreCase = true) }
                ?: apkCandidates.firstOrNull { it.name.contains("debug", ignoreCase = true) }
                ?: apkCandidates.firstOrNull { it.name.contains("release", ignoreCase = true) }
                ?: apkCandidates.first()
        }
    }
}
