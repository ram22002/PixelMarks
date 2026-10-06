package com.pixelmarks.core.util

import java.net.URI
import java.util.Locale

/**
 * Deterministic URL normalization, validation, and domain extraction utility.
 * Operates strictly locally without network access.
 */
object UrlUtils {

    /**
     * Validates whether the given string is a valid web URL.
     * Accepts URLs with or without scheme (defaults to https).
     */
    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return false

        val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }

        return runCatching {
            val uri = URI(withScheme)
            val host = uri.host
            !host.isNullOrBlank() &&
                (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
                host.contains(".")
        }.getOrDefault(false)
    }

    /**
     * Normalizes a URL for canonical storage and duplicate detection.
     * - Trims whitespace
     * - Prepends https:// if scheme is missing
     * - Lowercases scheme and host
     * - Drops default ports (80 for http, 443 for https)
     * - Strips redundant trailing slashes
     * - Preserves path, query parameters, and fragments
     */
    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotEmpty()) { "URL cannot be blank" }

        val withScheme = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val uri = URI(withScheme)
        val scheme = (uri.scheme ?: "https").lowercase(Locale.ROOT)
        val host = (uri.host ?: "").lowercase(Locale.ROOT)
        val port = if (uri.port != -1 &&
            !(scheme == "http" && uri.port == 80) &&
            !(scheme == "https" && uri.port == 443)
        ) {
            ":${uri.port}"
        } else {
            ""
        }

        var path = uri.rawPath ?: ""
        if (path.length > 1 && path.endsWith("/")) {
            path = path.dropLast(1)
        } else if (path == "/") {
            path = ""
        }

        val query = if (!uri.rawQuery.isNullOrBlank()) "?${uri.rawQuery}" else ""
        val fragment = if (!uri.rawFragment.isNullOrBlank()) "#${uri.rawFragment}" else ""

        return "$scheme://$host$port$path$query$fragment"
    }

    /**
     * Extracts a clean domain/host name from a URL for display and grouping.
     * Strips leading 'www.' if present.
     */
    fun extractDomain(url: String): String {
        val normalized = runCatching { normalizeUrl(url) }.getOrDefault(url.trim())
        return runCatching {
            val uri = URI(normalized)
            val host = uri.host ?: ""
            if (host.startsWith("www.", ignoreCase = true)) {
                host.substring(4)
            } else {
                host
            }
        }.getOrDefault("")
    }

    private val URL_REGEX = Regex("""(?i)\bhttps?://[^\s<>"{}|\\^`]+""")

    /**
     * Extracts the first valid HTTP/HTTPS URL from arbitrary shared text.
     * Supports multiline captions, text with surrounding copy, and strips trailing punctuation.
     * Rejects non-web schemes and invalid URLs.
     */
    fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val trimmed = text.trim()

        // 1. First look for explicit http:// or https:// matches
        val match = URL_REGEX.find(trimmed)
        if (match != null) {
            val candidate = match.value.trimEnd('.', ',', ';', '!', '?', ')', ']', '>', '"', '\'')
            if (isValidUrl(candidate)) {
                return runCatching { normalizeUrl(candidate) }.getOrNull()
            }
        }

        // 2. Fall back to inspecting individual whitespace-delimited tokens
        val tokens = trimmed.split(Regex("""\s+"""))
        for (token in tokens) {
            val candidate = token.trimEnd('.', ',', ';', '!', '?', ')', ']', '>', '"', '\'')
            if (isValidUrl(candidate)) {
                return runCatching { normalizeUrl(candidate) }.getOrNull()
            }
        }

        return null
    }
}
