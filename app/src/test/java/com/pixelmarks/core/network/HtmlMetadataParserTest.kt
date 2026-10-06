package com.pixelmarks.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HtmlMetadataParserTest {

    private lateinit var parser: HtmlMetadataParser
    private val baseUri = "https://example.com/blog/article-1"

    @Before
    fun setUp() {
        parser = HtmlMetadataParser()
    }

    @Test
    fun parse_ogTitle_preferredOverOthers() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Standard Page Title</title>
                <meta name="twitter:title" content="Twitter Title" />
                <meta property="og:title" content="OpenGraph Title" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("OpenGraph Title", metadata.title)
    }

    @Test
    fun parse_twitterTitle_preferredWhenOgTitleMissing() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Standard Page Title</title>
                <meta name="twitter:title" content="Twitter Title" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("Twitter Title", metadata.title)
    }

    @Test
    fun parse_standardTitle_fallbackWhenOgAndTwitterMissing() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Standard Page Title</title>
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("Standard Page Title", metadata.title)
    }

    @Test
    fun parse_ogDescription_preferredOverStandardAndTwitter() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="description" content="Meta Description" />
                <meta name="twitter:description" content="Twitter Description" />
                <meta property="og:description" content="OpenGraph Description" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("OpenGraph Description", metadata.description)
    }

    @Test
    fun parse_metaDescription_preferredWhenOgMissing() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="description" content="Meta Description" />
                <meta name="twitter:description" content="Twitter Description" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("Meta Description", metadata.description)
    }

    @Test
    fun parse_twitterDescription_preferredWhenOgAndMetaMissing() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="twitter:description" content="Twitter Description" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("Twitter Description", metadata.description)
    }

    @Test
    fun parse_ogImage_resolvedToAbsoluteUrl() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:image" content="/images/preview.png" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("https://example.com/images/preview.png", metadata.imageUrl)
    }

    @Test
    fun parse_twitterImage_whenOgMissing() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="twitter:image" content="https://cdn.example.com/card.jpg" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("https://cdn.example.com/card.jpg", metadata.imageUrl)
    }

    @Test
    fun parse_favicon_linkIcon() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <link rel="icon" href="/static/custom-favicon.png" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("https://example.com/static/custom-favicon.png", metadata.faviconUrl)
    }

    @Test
    fun parse_favicon_shortcutIcon() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <link rel="shortcut icon" href="https://example.com/shortcut.ico" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("https://example.com/shortcut.ico", metadata.faviconUrl)
    }

    @Test
    fun parse_favicon_appleTouchIconFallback() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <link rel="apple-touch-icon" href="/touch-icon.png" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        assertEquals("https://example.com/touch-icon.png", metadata.faviconUrl)
    }

    @Test
    fun parse_favicon_fallbackToDomainRootFavicon() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Just a Title</title>
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, "https://example.com/path/to/page")
        assertEquals("https://example.com/favicon.ico", metadata.faviconUrl)
    }

    @Test
    fun parse_emptyOrBlankHtml_returnsFallbackFavicon() {
        val metadata = parser.parse("", "https://example.com")
        assertNull(metadata.title)
        assertNull(metadata.description)
        assertNull(metadata.imageUrl)
        assertEquals("https://example.com/favicon.ico", metadata.faviconUrl)
    }

    @Test
    fun parse_malformedHtml_doesNotCrash() {
        val malformedHtml = "<html><head><title>Unclosed title<meta property=\"og:title\" content=\"Found\"><"
        val metadata = parser.parse(malformedHtml, baseUri)
        assertEquals("Found", metadata.title)
    }

    @Test
    fun parse_rejectsDangerousSchemesForImage() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:image" content="javascript:alert(1)" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = parser.parse(html, baseUri)
        // javascript: must be rejected, and if fallback favicon exists it may use it or null
        assertTrue(metadata.imageUrl == null || !metadata.imageUrl!!.startsWith("javascript:"))
    }
}
