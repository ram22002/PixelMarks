package com.pixelmarks.core.update

import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseAsset
import com.pixelmarks.core.update.model.ReleaseChannel
import com.pixelmarks.core.update.model.UpdateCheckResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests covering the update system:
 * - Version comparisons and semantic versioning
 * - Release channel inference and filtering (Stable, Beta, Nightly)
 * - Update detection (New release available vs Up to date)
 * - Dismissed update suppression
 * - Offline and error state handling
 * - Asset filtering (APK vs other assets)
 */
class UpdateSystemTest {

    private fun createRelease(
        tagName: String,
        isDraft: Boolean = false,
        isPreRelease: Boolean = false,
        hasApk: Boolean = true,
        body: String = "Release notes"
    ): GitHubRelease {
        val channel = ReleaseChannel.fromTag(tagName)
        val assets = if (hasApk) {
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
            publishedAt = "2026-11-01T12:00:00Z",
            htmlUrl = "https://github.com/ram22002/PixelMarks/releases/tag/$tagName",
            assets = assets,
            channel = channel,
            isDraft = isDraft,
            isPreRelease = isPreRelease
        )
    }

    // ── 1. SemVer comparison tests ──────────────────────────────────────────

    @Test
    fun `isNewer correctly compares semantic versions`() {
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(emptyList()),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        assertTrue(checker.isNewer("1.0.1", "1.0.0"))
        assertTrue(checker.isNewer("1.1.0", "1.0.9"))
        assertTrue(checker.isNewer("2.0.0", "1.99.99"))
        assertTrue(checker.isNewer("v1.0.1", "v1.0.0"))

        assertFalse(checker.isNewer("1.0.0", "1.0.0"))
        assertFalse(checker.isNewer("0.9.9", "1.0.0"))
        assertFalse(checker.isNewer("1.0.0", "1.0.1"))
    }

    // ── 2. Channel inference tests ──────────────────────────────────────────

    @Test
    fun `fromTag infers release channel accurately`() {
        assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromTag("v1.0.0"))
        assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromTag("1.2.3"))
        assertEquals(ReleaseChannel.BETA, ReleaseChannel.fromTag("v1.1.0-beta.1"))
        assertEquals(ReleaseChannel.BETA, ReleaseChannel.fromTag("v2.0.0-rc1"))
        assertEquals(ReleaseChannel.BETA, ReleaseChannel.fromTag("v1.0.0-alpha"))
        assertEquals(ReleaseChannel.NIGHTLY, ReleaseChannel.fromTag("v2026.11.03-nightly"))
    }

    // ── 3. Update detection: New stable release available ───────────────────

    @Test
    fun `checkForUpdate detects newer stable release`() = runBlocking {
        val releases = listOf(
            createRelease("v1.1.0"),
            createRelease("v1.0.0")
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val update = result as UpdateCheckResult.UpdateAvailable
        assertEquals("1.1.0", update.release.versionName)
    }

    // ── 4. Update detection: App is up to date ──────────────────────────────

    @Test
    fun `checkForUpdate returns UpToDate when installed version is latest or newer`() = runBlocking {
        val releases = listOf(
            createRelease("v1.0.0")
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 5. Channel filtering: Stable channel ignores beta/prerelease ────────

    @Test
    fun `stable channel ignores beta and prereleases`() = runBlocking {
        val releases = listOf(
            createRelease("v1.2.0-beta.1", isPreRelease = true),
            createRelease("v1.0.0")
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 6. Channel filtering: Beta channel accepts beta releases ────────────

    @Test
    fun `beta channel accepts beta releases`() = runBlocking {
        val releases = listOf(
            createRelease("v1.2.0-beta.1", isPreRelease = true),
            createRelease("v1.0.0")
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.BETA)
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        assertEquals("1.2.0", (result as UpdateCheckResult.UpdateAvailable).release.versionName)
    }

    // ── 7. Dismissed version persistence suppression ────────────────────────

    @Test
    fun `checkForUpdate suppresses dismissed update`() = runBlocking {
        val releases = listOf(
            createRelease("v1.1.0")
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(
            channel = ReleaseChannel.STABLE,
            dismissedVersionName = "1.1.0"
        )
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 8. APK asset filtering ──────────────────────────────────────────────

    @Test
    fun `releases without APK asset are ignored for update`() = runBlocking {
        val releases = listOf(
            createRelease("v1.2.0", hasApk = false) // Only source zip, no APK
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 9. Draft releases are ignored ───────────────────────────────────────

    @Test
    fun `draft releases are ignored`() = runBlocking {
        val releases = listOf(
            createRelease("v2.0.0", isDraft = true)
        )
        val checker = UpdateChecker(
            releaseRepository = FakeReleaseRepository(releases),
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.UpToDate, result)
    }

    // ── 10. Offline error mapping ───────────────────────────────────────────

    @Test
    fun `network failure maps to Offline result`() = runBlocking {
        val failingRepo = object : GitHubReleaseRepository {
            override suspend fun getReleases(): Result<List<GitHubRelease>> =
                Result.failure(IOException("No internet connection"))

            override suspend fun getReleasesForChannel(channel: ReleaseChannel): Result<List<GitHubRelease>> =
                Result.failure(IOException("No internet connection"))

            override fun invalidateCache() = Unit
        }

        val checker = UpdateChecker(
            releaseRepository = failingRepo,
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertEquals(UpdateCheckResult.Offline, result)
    }

    // ── 11. Rate limit error mapping ────────────────────────────────────────

    @Test
    fun `rate limit failure maps to Error result`() = runBlocking {
        val rateLimitedRepo = object : GitHubReleaseRepository {
            override suspend fun getReleases(): Result<List<GitHubRelease>> =
                Result.failure(IOException("GitHub API rate limit exceeded. Resets at: 1700000000"))

            override suspend fun getReleasesForChannel(channel: ReleaseChannel): Result<List<GitHubRelease>> =
                Result.failure(IOException("GitHub API rate limit exceeded. Resets at: 1700000000"))

            override fun invalidateCache() = Unit
        }

        val checker = UpdateChecker(
            releaseRepository = rateLimitedRepo,
            installedVersionName = "1.0.0",
            installedVersionCode = 1
        )

        val result = checker.checkForUpdate(ReleaseChannel.STABLE)
        assertTrue(result is UpdateCheckResult.Error)
        assertTrue((result as UpdateCheckResult.Error).message.contains("rate limit"))
    }

    // ── 12. Asset resolution helper ─────────────────────────────────────────

    @Test
    fun `apkAsset returns correct APK and ignores zip`() {
        val release = createRelease("v1.5.0", hasApk = true)
        assertNotNull(release.apkAsset)
        assertTrue(release.apkAsset!!.name.endsWith(".apk"))

        val releaseNoApk = createRelease("v1.5.0", hasApk = false)
        assertNull(releaseNoApk.apkAsset)
    }

    // ── Fake repository implementation ──────────────────────────────────────

    private class FakeReleaseRepository(
        private val releases: List<GitHubRelease>
    ) : GitHubReleaseRepository {
        override suspend fun getReleases(): Result<List<GitHubRelease>> =
            Result.success(releases)

        override suspend fun getReleasesForChannel(channel: ReleaseChannel): Result<List<GitHubRelease>> {
            val filtered = releases.filter { release ->
                when (channel) {
                    ReleaseChannel.STABLE -> !release.isPreRelease && release.channel == ReleaseChannel.STABLE
                    ReleaseChannel.BETA -> release.channel == ReleaseChannel.BETA || release.channel == ReleaseChannel.STABLE
                    ReleaseChannel.NIGHTLY -> true
                }
            }
            return Result.success(filtered)
        }

        override fun invalidateCache() = Unit
    }
}

