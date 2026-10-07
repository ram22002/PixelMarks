package com.pixelmarks.core.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Centralized manager for validating, resolving, and launching URLs based on user browser preferences.
 */
object BrowserLauncher {

    /**
     * Resolves the user's link-opening choice and triggers the appropriate navigation or intent.
     */
    fun openUrl(
        context: Context,
        url: String,
        preference: BrowserPreference,
        onNavigateToInApp: (String) -> Unit,
        onShowAskEveryTimeSheet: (String) -> Unit
    ) {
        if (!WebUrlValidator.isValidWebUrl(url)) {
            Toast.makeText(context, "Invalid or unsupported web URL", Toast.LENGTH_SHORT).show()
            return
        }
        val sanitized = WebUrlValidator.sanitizeUrl(url)

        when (preference) {
            BrowserPreference.IN_APP -> {
                onNavigateToInApp(sanitized)
            }
            BrowserPreference.SYSTEM_PREFERRED -> {
                launchSystemPreferred(context, sanitized)
            }
            BrowserPreference.DEFAULT_BROWSER -> {
                launchDefaultBrowser(context, sanitized)
            }
            BrowserPreference.ASK_EVERY_TIME -> {
                onShowAskEveryTimeSheet(sanitized)
            }
        }
    }

    /**
     * Launches the URL via Android Custom Tabs, allowing Android / system provider to decide.
     * Gracefully falls back to the default external browser if no Custom Tabs provider is available.
     */
    fun launchSystemPreferred(context: Context, url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            launchDefaultBrowser(context, url)
        }
    }

    /**
     * Resolves the user's current default browser package dynamically.
     * Does NOT hardcode any specific browser package name like com.android.chrome.
     */
    fun resolveDefaultBrowserPackage(context: Context): String? {
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.resolveActivity(
                    browserIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            val packageName = resolveInfo?.activityInfo?.packageName
            if (!packageName.isNullOrBlank() &&
                packageName != "android" &&
                packageName != context.packageName
            ) {
                packageName
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Launches the URL using standard Android ACTION_VIEW intent, targeting the user's default browser.
     */
    fun launchDefaultBrowser(context: Context, url: String) {
        val uri = Uri.parse(url)
        val defaultPackage = resolveDefaultBrowserPackage(context)

        // Try targeted intent first to guarantee opening in the user's default browser app
        if (defaultPackage != null) {
            try {
                val targetedIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(defaultPackage)
                }
                context.startActivity(targetedIntent)
                return
            } catch (_: Exception) {
                // Fall back to generic ACTION_VIEW below
            }
        }

        // Generic fallback if package resolution failed or targeted launch errored
        try {
            val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No browser app found to open link", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies the given URL to the Android system clipboard.
     */
    fun copyUrl(context: Context, url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("URL", url)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Opens Android share sheet with title and URL.
     */
    fun shareUrl(context: Context, title: String?, url: String) {
        val shareText = if (!title.isNullOrBlank()) "$title\n\n$url" else url
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(sendIntent, "Share link").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to share link", Toast.LENGTH_SHORT).show()
        }
    }
}
