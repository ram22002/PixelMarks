package com.pixelmarks.core.update.model

/**
 * A GitHub release record, mapped from the Releases API response.
 *
 * @param tagName    e.g. "v1.2.0" or "v1.2.0-beta.1"
 * @param name       Human-readable release title
 * @param body       Markdown release notes
 * @param publishedAt ISO-8601 timestamp string
 * @param htmlUrl    URL of the release page on GitHub
 * @param assets     APK and other attached files
 * @param channel    Inferred release channel
 * @param isDraft    Draft releases are never shown
 * @param isPreRelease GitHub prerelease flag (betas/nightlies)
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
    /** Semver string, stripping leading "v". */
    val versionName: String get() = tagName.trimStart('v').substringBefore('-').trim()

    /** APK asset for this release, or null if none is attached. */
    val apkAsset: ReleaseAsset?
        get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}

