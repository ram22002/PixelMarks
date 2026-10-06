package com.pixelmarks.core.network

import com.pixelmarks.core.model.PageMetadata
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.Locale

/**
 * Isolated, deterministic HTML metadata parser.
 * Extracts OpenGraph, Twitter, and standard HTML metadata following priority rules.
 */
class HtmlMetadataParser {

    companion object {
        const val MAX_TITLE_LENGTH = 300
        const val MAX_DESCRIPTION_LENGTH = 1000
    }

    /**
     * Parse HTML string and extract [PageMetadata] resolved against [baseUri].
     */
    fun parse(html: String, baseUri: String): PageMetadata {
        if (html.isBlank()) {
            return PageMetadata(
                faviconUrl = resolveFallbackFavicon(baseUri)
            )
        }

        val doc: Document = try {
            Jsoup.parse(html, baseUri)
        } catch (e: Exception) {
            return PageMetadata(
                faviconUrl = resolveFallbackFavicon(baseUri)
            )
        }

        val title = extractTitle(doc)
        val description = extractDescription(doc)
        val faviconUrl = extractFavicon(doc, baseUri)
        val imageUrl = extractImage(doc, baseUri, faviconUrl)

        return PageMetadata(
            title = title,
            description = description,
            imageUrl = imageUrl,
            faviconUrl = faviconUrl
        )
    }

    private fun extractTitle(doc: Document): String? {
        // 1. og:title
        getMetaContent(doc, "og:title")?.let { return sanitize(it, MAX_TITLE_LENGTH) }

        // 2. twitter:title
        getMetaContent(doc, "twitter:title")?.let { return sanitize(it, MAX_TITLE_LENGTH) }

        // 3. <title> tag
        val titleText = doc.title().trim()
        if (titleText.isNotBlank()) {
            return sanitize(titleText, MAX_TITLE_LENGTH)
        }

        return null
    }

    private fun extractDescription(doc: Document): String? {
        // 1. og:description
        getMetaContent(doc, "og:description")?.let { return sanitize(it, MAX_DESCRIPTION_LENGTH) }

        // 2. meta[name="description"]
        getMetaContent(doc, "description")?.let { return sanitize(it, MAX_DESCRIPTION_LENGTH) }

        // 3. twitter:description
        getMetaContent(doc, "twitter:description")?.let { return sanitize(it, MAX_DESCRIPTION_LENGTH) }

        return null
    }

    private fun extractImage(doc: Document, baseUri: String, fallbackFavicon: String?): String? {
        // 1. og:image
        getMetaContent(doc, "og:image")?.let { raw ->
            resolveUrl(baseUri, raw)?.let { return it }
        }

        // 2. twitter:image
        getMetaContent(doc, "twitter:image")?.let { raw ->
            resolveUrl(baseUri, raw)?.let { return it }
        }

        // 3. Suitable page image: <link rel="image_src">
        val linkImage = doc.selectFirst("link[rel~=(?i)^image_src$]")?.attr("href")
        if (!linkImage.isNullOrBlank()) {
            resolveUrl(baseUri, linkImage)?.let { return it }
        }

        // 3b. Suitable page image: first meaningful <img> in body
        val firstImg = doc.select("body img[src]").firstOrNull { img ->
            val src = img.attr("src")
            !src.startsWith("data:") && !src.contains("tracking", ignoreCase = true)
        }?.attr("src")
        if (!firstImg.isNullOrBlank()) {
            resolveUrl(baseUri, firstImg)?.let { return it }
        }

        // 4. apple-touch-icon
        val appleIcon = doc.selectFirst("link[rel~=(?i)^apple-touch-icon.*]")?.attr("href")
        if (!appleIcon.isNullOrBlank()) {
            resolveUrl(baseUri, appleIcon)?.let { return it }
        }

        // 5. favicon fallback if found
        if (fallbackFavicon != null) {
            return fallbackFavicon
        }

        return null
    }

    private fun extractFavicon(doc: Document, baseUri: String): String? {
        // 1. <link rel="icon">
        val iconHref = doc.selectFirst("link[rel~=(?i)^icon$]")?.attr("href")
        if (!iconHref.isNullOrBlank()) {
            resolveUrl(baseUri, iconHref)?.let { return it }
        }

        // 2. <link rel="shortcut icon">
        val shortcutHref = doc.selectFirst("link[rel~=(?i)^shortcut icon$]")?.attr("href")
        if (!shortcutHref.isNullOrBlank()) {
            resolveUrl(baseUri, shortcutHref)?.let { return it }
        }

        // 3. <link rel="apple-touch-icon">
        val appleHref = doc.selectFirst("link[rel~=(?i)^apple-touch-icon.*]")?.attr("href")
        if (!appleHref.isNullOrBlank()) {
            resolveUrl(baseUri, appleHref)?.let { return it }
        }

        // 4. Default fallback: /favicon.ico on origin
        return resolveFallbackFavicon(baseUri)
    }

    private fun resolveFallbackFavicon(baseUri: String): String? {
        return try {
            val uri = URI(baseUri)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: return null
            val port = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            "$scheme://$host$port/favicon.ico"
        } catch (e: Exception) {
            null
        }
    }

    private fun getMetaContent(doc: Document, vararg keys: String): String? {
        for (key in keys) {
            // Check property attribute
            val byProperty = doc.selectFirst("meta[property~=(?i)^\\Q$key\\E$]")
            val contentProperty = byProperty?.attr("content")?.trim()
            if (!contentProperty.isNullOrBlank()) {
                return contentProperty
            }

            // Check name attribute
            val byName = doc.selectFirst("meta[name~=(?i)^\\Q$key\\E$]")
            val contentName = byName?.attr("content")?.trim()
            if (!contentName.isNullOrBlank()) {
                return contentName
            }
        }
        return null
    }

    private fun sanitize(input: String, maxLength: Int): String? {
        val trimmed = input.trim().replace("\\s+".toRegex(), " ")
        if (trimmed.isEmpty()) return null
        return if (trimmed.length > maxLength) {
            trimmed.take(maxLength).trimEnd()
        } else {
            trimmed
        }
    }

    private fun resolveUrl(baseUri: String, relativeOrAbsolute: String?): String? {
        if (relativeOrAbsolute.isNullOrBlank()) return null
        val trimmed = relativeOrAbsolute.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Reject forbidden schemes
        if (lower.startsWith("javascript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("file:") ||
            lower.startsWith("content:")
        ) {
            return null
        }

        return try {
            val base = URI(baseUri)
            val resolved = base.resolve(trimmed).toString()
            val lowerResolved = resolved.lowercase(Locale.ROOT)
            if (lowerResolved.startsWith("http://") || lowerResolved.startsWith("https://")) {
                resolved
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
