package com.pixelmarks.core.browser

import java.util.Locale

/**
 * Strict validator for web URLs to prevent security vulnerabilities and scheme injection.
 */
object WebUrlValidator {

    /**
     * Validates whether [url] is a legitimate, remote HTTP or HTTPS web URL.
     */
    fun isValidWebUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Only allow HTTP and HTTPS
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false
        }

        // Explicitly reject dangerous or local schemes
        if (lower.startsWith("javascript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("content:") ||
            lower.startsWith("data:") ||
            lower.startsWith("intent:") ||
            lower.startsWith("chrome:") ||
            lower.startsWith("about:")
        ) {
            return false
        }

        // Reject strings with unencoded spaces or that are too short to be valid
        if (trimmed.contains(" ") || trimmed.length < 8) {
            return false
        }

        return true
    }

    /**
     * Trims leading/trailing whitespace from [url].
     */
    fun sanitizeUrl(url: String): String {
        return url.trim()
    }
}
