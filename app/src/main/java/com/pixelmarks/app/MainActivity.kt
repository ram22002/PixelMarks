package com.pixelmarks.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Main Activity host for PixelMarks.
 * Handles app launch, intent navigation (e.g. from reminder notification clicks),
 * and hosts PixelMarksApp.
 */
class MainActivity : ComponentActivity() {

    private var initialBookmarkId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setContent {
            PixelMarksApp(initialBookmarkId = initialBookmarkId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val bId = intent?.getLongExtra("bookmarkId", -1L) ?: -1L
        if (bId > 0) {
            initialBookmarkId = bId
        }
    }
}
