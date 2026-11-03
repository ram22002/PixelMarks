package com.pixelmarks.core.update

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pixelmarks.core.update.model.GitHubRelease
import com.pixelmarks.core.update.model.ReleaseAsset
import com.pixelmarks.core.update.model.ReleaseChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Fetches PixelMarks release data from the public GitHub Releases API.
 *
 * No authentication token required for public repositories.
 * Rate limit: 60 requests/hour unauthenticated (checked via X-RateLimit-Remaining header).
 * Results are cached in-memory for [CACHE_DURATION_MS] to avoid redundant requests.
 *
 * Repository: https://github.com/ram22002/PixelMarks
 */
class GitHubReleaseRepositoryImpl(
    private val okHttpClient: OkHttpClient = buildClient(),
    private val gson: Gson = Gson()
) : GitHubReleaseRepository {

    companion object {
        private const val OWNER = "ram22002"
        private const val REPO = "PixelMarks"
        private const val RELEASES_URL =
            "https://api.github.com/repos/$OWNER/$REPO/releases?per_page=50"

        fun buildClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    // ---- In-memory cache ----
    private var cachedReleases: List<GitHubRelease>? = null
    private var cacheTimestamp: Long = 0L

    override suspend fun getReleases(): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cachedReleases
        if (cached != null && now - cacheTimestamp < GitHubReleaseRepository.CACHE_DURATION_MS) {
            return@withContext Result.success(cached)
        }

        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "PixelMarks-Android/1.0")
            .build()

        return@withContext try {
            okHttpClient.newCall(request).execute().use { response ->
                when {
                    response.code == 403 || response.code == 429 -> {
                        val resetAt = response.header("X-RateLimit-Reset")
                        Result.failure(IOException("GitHub API rate limit exceeded. Resets at: $resetAt"))
                    }
                    !response.isSuccessful -> {
                        Result.failure(IOException("GitHub API error: HTTP ${response.code}"))
                    }
                    else -> {
                        val body = response.body?.string()
                            ?: return@withContext Result.failure(IOException("Empty response"))
                        val releases = parseReleases(body)
                        cachedReleases = releases
                        cacheTimestamp = System.currentTimeMillis()
                        Result.success(releases)
                    }
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(IOException("No internet connection", e))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected error: ${e.message}", e))
        }
    }

    override suspend fun getReleasesForChannel(channel: ReleaseChannel): Result<List<GitHubRelease>> {
        return getReleases().map { releases ->
            releases.filter { release ->
                when (channel) {
                    ReleaseChannel.STABLE -> !release.isPreRelease && release.channel == ReleaseChannel.STABLE
                    ReleaseChannel.BETA -> release.channel == ReleaseChannel.BETA || release.channel == ReleaseChannel.STABLE
                    ReleaseChannel.NIGHTLY -> true // All releases
                }
            }
        }
    }

    override fun invalidateCache() {
        cachedReleases = null
        cacheTimestamp = 0L
    }

    // ---- JSON parsing ----
    private fun parseReleases(json: String): List<GitHubRelease> {
        val array = gson.fromJson(json, JsonArray::class.java)
        return array.mapNotNull { element ->
            try {
                val obj = element.asJsonObject
                val isDraft = obj.get("draft")?.asBoolean ?: false
                if (isDraft) return@mapNotNull null // Skip drafts entirely

                val tagName = obj.get("tag_name")?.asString ?: return@mapNotNull null
                val channel = ReleaseChannel.fromTag(tagName)
                val isPreRelease = obj.get("prerelease")?.asBoolean ?: false

                GitHubRelease(
                    id = obj.get("id")?.asLong ?: 0L,
                    tagName = tagName,
                    name = obj.get("name")?.asString ?: tagName,
                    body = obj.get("body")?.asString ?: "",
                    publishedAt = obj.get("published_at")?.asString ?: "",
                    htmlUrl = obj.get("html_url")?.asString ?: "",
                    assets = parseAssets(obj.getAsJsonArray("assets")),
                    channel = channel,
                    isDraft = false,
                    isPreRelease = isPreRelease
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseAssets(assetsArray: JsonArray?): List<ReleaseAsset> {
        if (assetsArray == null) return emptyList()
        return assetsArray.mapNotNull { element ->
            try {
                val obj = element.asJsonObject
                ReleaseAsset(
                    name = obj.get("name")?.asString ?: return@mapNotNull null,
                    downloadUrl = obj.get("browser_download_url")?.asString ?: return@mapNotNull null,
                    sizeBytes = obj.get("size")?.asLong ?: 0L,
                    contentType = obj.get("content_type")?.asString ?: "application/octet-stream"
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

