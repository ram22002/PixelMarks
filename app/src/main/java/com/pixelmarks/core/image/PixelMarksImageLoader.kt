package com.pixelmarks.core.image

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Centralized Coil [ImageLoader] factory providing memory and disk caching
 * tailored for PixelMarks.
 */
object PixelMarksImageLoader {

    private const val MAX_DISK_CACHE_BYTES = 50L * 1024L * 1024L // 50 MB
    private const val DISK_CACHE_DIR_NAME = "image_cache"

    /**
     * Builds an application-level [ImageLoader] configured with:
     * - In-memory cache capped at 25% of available app memory.
     * - Private disk cache in the application cache directory capped at 50 MB.
     * - Dedicated OkHttpClient with safe timeouts.
     * - Crossfade enabled for smooth rendering.
     */
    fun create(context: Context): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_DIR_NAME))
                    .maxSizeBytes(MAX_DISK_CACHE_BYTES)
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .crossfade(true)
            .build()
    }
}
