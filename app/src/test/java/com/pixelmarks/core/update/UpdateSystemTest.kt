package com.pixelmarks.core.update

import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseAsset
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.SemVer
import com.pixelmarks.core.update.model.UpdateCheckResult
import com.pixelmarks.feature.changelog.buildAnnotatedMarkdown
import com.pixelmarks.feature.changelog.sanitizeReleaseBody
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Deterministic unit tests covering:
 * 1. Installed v1.0.0-beta and latest v1.0.1-beta (Update available)
 * 2. Installed v1.0.1-beta and latest v1.0.0-beta (No downgrade offered / Up to date)
 * 3. Stable versus beta releases (Channel filtering)
 * 4. Empty release body handling
 * 5. Markdown release notes rendering & formatting
 * 6. Draft release exclusion
 * 7. Missing APK asset (ApkAssetMissing state)
 * 8. ZIP/TAR.GZ/SVG exclusion
 * 9. Multiple APK assets resolution priority
 * 10. GitHub API failure and rate limiting
 * 11. Stale cache followed by a newer release (Cache invalidation)
 * 12. Invalid version strings (Graceful fallback)
 * 13. No releases available (NoReleasesFound state)
 */
class UpdateSystemTest {

    private fun createRelease(
        tagName: String,
        isDraft: Boolean = false,
        isPreRelease: Boolean = false,
        hasApk: Boolean = true,
        body: String = "Release notes",
        assets: List<ReleaseAsset>? = null
    ): GitHubRelease {
        val channel = ReleaseChannel.fromTag(tagName, isPreRelease)
        val finalAssets = assets ?: if (hasApk) {
            listOf(
                ReleaseAsset(
                    name = "PixelMarks-${tagName.trimStart('v')}.apk",
                    downloadUrl = "https://github.com/ram22002/PixelMarks/releases/download/$tagName/app.apk",
                    sizeBytes = 25_000_000L,
                    contentType = "application/vnd.android.package-archive"
                )
            )
        } else {
            listOf(
                ReleaseAsset(
                    name = "source-code.zip",
                    downloadUrl = "https://github.com/ram22002/PixelMarks/archive/refs/tags/$tagName.zip",
                    sizeBytes = 1_000_000L,
                    contentType = "application/zip"
                )
            )
        }

        return GitHubRelease(
            id = 1L,
            tagName = tagName,
            name = "PixelMarks $tagName",
            body = body,
            publishedAt = "2026-11-03T12:00:00Z",
            htmlUrl = "https://github.com/ram22002/PixelMarks/releases/tag/$tagName",
            assets = finalAssets,
            channel = channel,
            isDraft = isDraft,
            isPreRelease = isPreRelease
        )
    }

    // ── 1. Installed v1.0.0-beta and latest v1.0.1-beta ──────────────────────

    @Test
    fun `testScenario1_installedBeta_detectsNewerBetaRelease`() = runBlocking {
        val releases = listOf(
            createRelease("v1.0.1-beta", isPreRelease = true),
            createRelease("v1.0.0-beta", isPreRelease = true)
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0-beta",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.BETA)
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val update = result as UpdateCheckResult.UpdateAvailable
        assertEquals("1.0.1-beta", update.release.versionName)
    }

    // ── 2. Installed v1.0.1-beta and latest v1.0.0-beta ──────────────────────

    @Test
    fun `testScenario2_installedNewerBeta_doesNotOfferDowngrade`() = runBlocking {
        val releases = listOf(
            createRelease("v1.0.0-beta", isPreRelease = true)
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.1-beta",
            installedVersionCode = 2
        )

        val result = checker.checkForUpdate(ReleaseChannel.BETA)
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 3. Stable versus beta releases ───────────────────────────────────────

    @Test
    fun `testScenario3_stableChannelIgnoresBeta_betaChannelAcceptsBoth`() = runBlocking {
        val releases = listOf(
            createRelease("v1.1.0-beta.1", isPreRelease = true),
            createRelease("v1.0.0", isPreRelease = false)
        )
        val repo = FakeReleaseRepository(releases)

        // Stable channel: ignores v1.1.0-beta.1, sees v1.0.0
        val stableChecker = UpdateChecker(repo, installedVersionName = "1.0.0", installedVersionCode = 1)
        val stableResult = stableChecker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, stableResult)

        // Beta channel: accepts v1.1.0-beta.1 over v1.0.0
        val betaResult = stableChecker.checkForUpdate(ReleaseChannel.BETA)
        assertTrue(betaResult is UpdateCheckResult.UpdateAvailable)
        assertEquals("1.1.0-beta.1", (betaResult as UpdateCheckResult.UpdateAvailable).release.versionName)
    }

    // ── 4. Empty release body ───────────────────────────────────────────────

    @Test
    fun `testScenario4_emptyReleaseBody_handledCleanly`() {
        val releaseEmpty = createRelease("v1.0.1-beta", body = "")
        assertFalse(releaseEmpty.hasReleaseNotes)

        val sanitized = sanitizeReleaseBody(releaseEmpty.body)
        assertEquals("", sanitized)

        val releaseWhitespace = createRelease("v1.0.1-beta", body = "   \n\t  ")
        assertEquals("", sanitizeReleaseBody(releaseWhitespace.body))
    }

    // ── 5. Markdown release notes ───────────────────────────────────────────

    @Test
    fun `testScenario5_markdownReleaseNotesParsing`() {
        val markdown = """
            ## Highlights
            - **Performance**: Faster database queries with `Room 2.6`
            - *Security*: AES-256 encryption upgraded
            - [Changelog](https://github.com/ram22002/PixelMarks)
            1. First item
            2. Second item
        """.trimIndent()

        val release = createRelease("v1.0.1", body = markdown)
        assertTrue(release.hasReleaseNotes)

        val sanitized = sanitizeReleaseBody(release.body)
        assertTrue(sanitized.contains("Performance"))

        val annotated = buildAnnotatedMarkdown(sanitized)
        assertTrue(annotated.text.contains("Faster database queries with"))
        assertTrue(annotated.text.contains("Changelog"))
    }

    // ── 6. Draft release exclusion ──────────────────────────────────────────

    @Test
    fun `testScenario6_draftReleasesExcluded`() = runBlocking {
        val releases = listOf(
            createRelease("v2.0.0", isDraft = true),
            createRelease("v1.0.0", isDraft = false)
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 7. Missing APK asset ────────────────────────────────────────────────

    @Test
    fun `testScenario7_missingApkAsset_reportsApkAssetMissing`() = runBlocking {
        val releases = listOf(
            createRelease("v1.2.0", hasApk = false) // Only source archive zip
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertTrue(result is UpdateCheckResult.ApkAssetMissing)
        val missing = result as UpdateCheckResult.ApkAssetMissing
        assertEquals("1.2.0", missing.release.versionName)
    }

    // ── 8. ZIP / TAR.GZ / SVG exclusion ─────────────────────────────────────

    @Test
    fun `testScenario8_zipTarGzSvgExcludedFromApkSelection`() {
        val mixedAssets = listOf(
            ReleaseAsset("icon.svg", "https://ex.com/icon.svg", 1024L, "image/svg+xml"),
            ReleaseAsset("source.zip", "https://ex.com/src.zip", 2048L, "application/zip"),
            ReleaseAsset("source.tar.gz", "https://ex.com/src.tar.gz", 2048L, "application/gzip"),
            ReleaseAsset("checksums.sha256", "https://ex.com/c.sha256", 64L, "text/plain"),
            ReleaseAsset("app-release.apk", "https://ex.com/app.apk", 25_000_000L, "application/vnd.android.package-archive")
        )

        val selected = GitHubRelease.selectCompatibleApk(mixedAssets)
        assertNotNull(selected)
        assertEquals("app-release.apk", selected!!.name)

        // Without any APK:
        val noApks = mixedAssets.dropLast(1)
        assertNull(GitHubRelease.selectCompatibleApk(noApks))
    }

    // ── 9. Multiple APK assets ──────────────────────────────────────────────

    @Test
    fun `testScenario9_multipleApkAssetsSelectionPriority`() {
        val multipleAssets = listOf(
            ReleaseAsset("app-arm64.apk", "https://ex.com/arm64.apk", 20_000_000L, "application/vnd.android.package-archive"),
            ReleaseAsset("app-universal.apk", "https://ex.com/uni.apk", 35_000_000L, "application/vnd.android.package-archive"),
            ReleaseAsset("app-x86.apk", "https://ex.com/x86.apk", 22_000_000L, "application/vnd.android.package-archive")
        )

        val selected = GitHubRelease.selectCompatibleApk(multipleAssets)
        assertNotNull(selected)
        assertEquals("app-universal.apk", selected!!.name)
    }

    // ── 10. GitHub API failure and rate limiting ────────────────────────────

    @Test
    fun `testScenario10_apiRateLimitAndOfflineFailureMapping`() = runBlocking {
        val rateLimitRepo = object : GitHubReleaseRepository {
            override suspend fun getReleases(forceRefresh: Boolean): Result<List<GitHubRelease>> =
                Result.failure(IOException("GitHub API rate limit exceeded. Resets at: 1700000000"))

            override suspend fun getReleasesForChannel(channel: ReleaseChannel, forceRefresh: Boolean): Result<List<GitHubRelease>> =
                Result.failure(IOException("GitHub API rate limit exceeded. Resets at: 1700000000"))

            override fun invalidateCache() = Unit
            override fun getCachedReleases(): List<GitHubRelease>? = null
        }

        val checker = UpdateChecker(rateLimitRepo, "1.0.0", 1)
        val rateLimitResult = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertTrue(rateLimitResult is UpdateCheckResult.RateLimited)

        val offlineRepo = object : GitHubReleaseRepository {
            override suspend fun getReleases(forceRefresh: Boolean): Result<List<GitHubRelease>> =
                Result.failure(IOException("No internet connection"))

            override suspend fun getReleasesForChannel(channel: ReleaseChannel, forceRefresh: Boolean): Result<List<GitHubRelease>> =
                Result.failure(IOException("No internet connection"))

            override fun invalidateCache() = Unit
            override fun getCachedReleases(): List<GitHubRelease>? = null
        }

        val offlineChecker = UpdateChecker(offlineRepo, "1.0.0", 1)
        val offlineResult = offlineChecker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.Offline, offlineResult)
    }

    // ── 11. Stale cache followed by a newer release ─────────────────────────

    @Test
    fun `testScenario11_staleCacheInvalidationRefreshesReleases`() = runBlocking {
        var currentReleases = listOf(createRelease("v1.0.0"))
        val repo = object : GitHubReleaseRepository {
            private var cached: List<GitHubRelease>? = null

            override suspend fun getReleases(forceRefresh: Boolean): Result<List<GitHubRelease>> {
                if (forceRefresh || cached == null) {
                    cached = currentReleases
                }
                return Result.success(cached!!)
            }

            override suspend fun getReleasesForChannel(channel: ReleaseChannel, forceRefresh: Boolean): Result<List<GitHubRelease>> {
                return getReleases(forceRefresh)
            }

            override fun invalidateCache() {
                cached = null
            }

            override fun getCachedReleases(): List<GitHubRelease>? = cached
        }

        val checker = UpdateChecker(repo, "1.0.0", 1)
        val initial = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, initial)

        // New release published on GitHub:
        currentReleases = listOf(createRelease("v1.0.1"), createRelease("v1.0.0"))

        // Stale check without cache invalidation:
        val stale = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, stale)

        // Cache invalidated:
        repo.invalidateCache()
        val refreshed = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertTrue(refreshed is UpdateCheckResult.UpdateAvailable)
        assertEquals("1.0.1", (refreshed as UpdateCheckResult.UpdateAvailable).release.versionName)
    }

    // ── 12. Invalid version strings ─────────────────────────────────────────

    @Test
    fun `testScenario12_invalidVersionStrings_handledGracefully`() {
        val checker = UpdateChecker(FakeReleaseRepository(emptyList()), "1.0.0", 1)

        // Should not crash and should fall back safely
        assertFalse(checker.isNewer("invalid", "1.0.0"))
        assertFalse(checker.isNewer("", "1.0.0"))
        assertFalse(checker.isNewer("not-a-version", "also-not"))

        val parsed = SemVer.parse("corrupted-tag")
        assertEquals(0, parsed.major)
        assertEquals(0, parsed.minor)
        assertEquals(0, parsed.patch)
    }

    // ── 13. No releases available ───────────────────────────────────────────

    @Test
    fun `testScenario13_noReleasesAvailable_reportsNoReleasesFound`() = runBlocking {
        val emptyRepo = FakeReleaseRepository(emptyList())
        val checker = UpdateChecker(emptyRepo, "1.0.0", 1)

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertTrue(result is UpdateCheckResult.NoReleasesFound)
        assertEquals(ReleaseChannel.STABLE, (result as UpdateCheckResult.NoReleasesFound).channel)
    }

    // ── Additional SemVer Precedence Tests ───────────────────────────────────

    @Test
    fun `semVerPrecedence_handlesRcAndBetaCorrectly`() {
        // v1.0.0-beta < v1.0.0 (stable is newer than prerelease)
        assertTrue(SemVer.parse("1.0.0") > SemVer.parse("1.0.0-beta"))

        // v1.0.0-beta.1 < v1.0.0-beta.2
        assertTrue(SemVer.parse("1.0.0-beta.2") > SemVer.parse("1.0.0-beta.1"))

        // v1.0.0-beta.2 < v1.0.0-beta.10
        assertTrue(SemVer.parse("1.0.0-beta.10") > SemVer.parse("1.0.0-beta.2"))

        // v1.0.0-rc.1 < v1.0.0
        assertTrue(SemVer.parse("1.0.0") > SemVer.parse("1.0.0-rc.1"))

        // v1.0.0-beta < v1.0.0-rc
        assertTrue(SemVer.parse("1.0.0-rc.1") > SemVer.parse("1.0.0-beta.1"))

        // Build metadata ignored
        assertEquals(0, SemVer.parse("1.0.0+1").compareTo(SemVer.parse("1.0.0+2")))
    }

    // ── Fake Repository Helper ──────────────────────────────────────────────

    private class FakeReleaseRepository(
        private val releases: List<GitHubRelease>
    ) : GitHubReleaseRepository {
        private var cached: List<GitHubRelease>? = releases

        override suspend fun getReleases(forceRefresh: Boolean): Result<List<GitHubRelease>> =
            Result.success(releases)

        override suspend fun getReleasesForChannel(channel: ReleaseChannel, forceRefresh: Boolean): Result<List<GitHubRelease>> {
            val filtered = releases.filter { release ->
                when (channel) {
                    ReleaseChannel.STABLE -> !release.isPreRelease && release.channel == ReleaseChannel.STABLE
                    ReleaseChannel.BETA -> release.channel == ReleaseChannel.BETA || release.channel == ReleaseChannel.STABLE
                    ReleaseChannel.NIGHTLY -> true
                }
            }
            return Result.success(filtered)
        }

        override fun invalidateCache() {
            cached = null
        }

        override fun getCachedReleases(): List<GitHubRelease>? = cached
    }
}
