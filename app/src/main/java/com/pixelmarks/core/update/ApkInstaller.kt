package com.pixelmarks.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Triggers the system package installer for a downloaded APK.
 *
 * Uses ACTION_VIEW with a DownloadManager content URI, which does NOT require
 * a FileProvider and works on all supported API levels (24+).
 *
 * If the user has not enabled "Install unknown apps" for this app, guides them
 * to the relevant Settings screen before attempting installation.
 *
 * Never silently installs — always uses the system installer UI.
 */
class ApkInstaller(private val context: Context) {

    /**
     * Returns true if this app is allowed to install packages from unknown sources.
     * Always true on API < 26 (Android 7 / 7.1 use global setting).
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Open Settings so the user can grant "Install unknown apps" permission for PixelMarks.
     * Only applicable on Android 8+ (API 26+).
     */
    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launch the system package installer UI for [apkUri].
     * [apkUri] must be a DownloadManager content URI (from [ApkDownloadManager]).
     *
     * The caller must ensure [canInstallPackages] returns true before calling this.
     */
    fun install(apkUri: Uri) {
        val uriToOpen = try {
            if (apkUri.scheme == "file") {
                val file = java.io.File(apkUri.path ?: "")
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                apkUri
            }
        } catch (_: Exception) {
            apkUri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriToOpen, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    /**
     * Open the PixelMarks GitHub releases page in the browser as a fallback
     * when direct APK installation is not possible.
     */
    fun openReleasePage(htmlUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

