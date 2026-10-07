package com.pixelmarks.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.pixelmarks.core.database.PixelMarksDatabase

/**
 * Main application class for PixelMarks.
 * Serves as the application-level context and entry point, and provides
 * the centralized Coil ImageLoader factory.
 */
class PixelMarksApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer

    val database: PixelMarksDatabase by lazy {
        PixelMarksDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        SingletonImageLoader.setSafe { container.imageLoader }
        com.pixelmarks.core.reminder.ReminderScheduler.createNotificationChannel(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return container.imageLoader
    }
}
