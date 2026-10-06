package com.pixelmarks.app

import android.app.Application
import com.pixelmarks.core.database.PixelMarksDatabase

/**
 * Main application class for PixelMarks.
 * Serves as the application-level context and entry point.
 */
class PixelMarksApplication : Application() {
    lateinit var container: AppContainer

    val database: PixelMarksDatabase by lazy {
        PixelMarksDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
