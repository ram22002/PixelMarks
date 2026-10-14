package com.pixelmarks.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pixelmarks.core.security.model.PrivacyMode
import kotlinx.coroutines.launch

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Main Activity host for PixelMarks.
 * Handles app launch, intent navigation (e.g. from reminder notification clicks),
 * biometric authentication hosting, and hosts PixelMarksApp.
 */
class MainActivity : FragmentActivity() {

    private var initialBookmarkId by mutableStateOf<Long?>(null)
    private var isAppReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val container = (application as? PixelMarksApplication)?.container
                container?.privateModeManager?.mode?.collect { mode ->
                    if (mode == PrivacyMode.PRIVATE) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }

        setContent {
            PixelMarksApp(
                initialBookmarkId = initialBookmarkId,
                onReady = { isAppReady = true }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        (application as? PixelMarksApplication)?.container?.appLockManager?.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            val container = (application as? PixelMarksApplication)?.container
            container?.appLockManager?.onAppBackgrounded()
            container?.privateModeManager?.onAppBackgrounded()
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
