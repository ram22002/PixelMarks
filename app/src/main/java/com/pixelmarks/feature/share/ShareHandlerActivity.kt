package com.pixelmarks.feature.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pixelmarks.core.util.UrlUtils
import com.pixelmarks.feature.bookmark.AddBookmarkScreen
import com.pixelmarks.ui.theme.PixelMarksTheme

/**
 * Dedicated Share Receiver Activity handling incoming Android ACTION_SEND intents.
 * Extracts web URLs from text shared by Instagram, YouTube, Reddit, Chrome, etc.,
 * and hosts the PixelMarks save bookmark flow.
 */
class ShareHandlerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractSharedText(intent)
        val extractedUrl = UrlUtils.extractUrlFromText(sharedText)

        if (extractedUrl == null) {
            Toast.makeText(this, "No valid web link found in shared content", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            PixelMarksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AddBookmarkScreen(
                        initialUrl = extractedUrl,
                        onNavigateBack = { finish() },
                        onNavigateToBookmarkDetail = { finish() },
                        onNavigateToEditBookmark = { finish() }
                    )
                }
            }
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null
        if (!type.startsWith("text/")) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
    }
}
