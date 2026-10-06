package com.pixelmarks.core.image

import java.util.Locale

/**
 * Utility for validating image and favicon URLs before attempting to load or cache them.
 *
 * Security requirements:
 * - Only allow http:// and https:// schemes.
 * - Explicitly reject file://, content://, javascript:, data:, intent:, and malformed paths.
 * - Reject blank or null values.
 */
object ImageUrlValidator {

    /**
     * Returns true if [url] is a safe, remote HTTP or HTTPS URL suitable for image loading.
     */
    fun isValidImageUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Strict protocol whitelist
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false
        }

        // Explicitly block local and script schemes
        if (lower.startsWith("file:") ||
            lower.startsWith("content:") ||
            lower.startsWith("javascript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("intent:") ||
            lower.startsWith("chrome:") ||
            lower.startsWith("about:")
        ) {
            return false
        }

        // Must have a valid length and not contain unescaped whitespace
        if (trimmed.length < 10 || trimmed.contains(" ")) {
            return false
        }

        return true
    }
}
