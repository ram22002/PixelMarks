package com.pixelmarks.core.image

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PixelMarksImageLoaderTest {

    @Test
    fun create_configuresImageLoaderWithMemoryAndDiskCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageLoader = PixelMarksImageLoader.create(context)

        assertNotNull(imageLoader)
        assertNotNull(imageLoader.memoryCache)
        assertNotNull(imageLoader.diskCache)
    }
}
