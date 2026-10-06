package com.pixelmarks.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractionTest {

    @Test
    fun extractUrl_fromInstagramShareCaption() {
        val sharedText = """
            Check this amazing reel!
            
            https://www.instagram.com/reel/abc123/
        """.trimIndent()

        val extracted = UrlUtils.extractUrlFromText(sharedText)
        assertEquals("https://www.instagram.com/reel/abc123", extracted)
    }

    @Test
    fun extractUrl_fromYouTubeShareText() {
        val sharedText = "Amazing video by Creator: https://youtu.be/dQw4w9WgXcQ check it out!"
        val extracted = UrlUtils.extractUrlFromText(sharedText)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", extracted)
    }

    @Test
    fun extractUrl_standaloneUrl() {
        assertEquals("https://example.com", UrlUtils.extractUrlFromText("https://example.com"))
        assertEquals("http://example.org/path", UrlUtils.extractUrlFromText("http://example.org/path/"))
    }

    @Test
    fun extractUrl_stripsTrailingPunctuation() {
        assertEquals(
            "https://example.com/article",
            UrlUtils.extractUrlFromText("Read this article at https://example.com/article.")
        )
        assertEquals(
            "https://example.com/page",
            UrlUtils.extractUrlFromText("Look at https://example.com/page, it is great!")
        )
        assertEquals(
            "https://example.com/docs",
            UrlUtils.extractUrlFromText("Check out (https://example.com/docs)")
        )
        assertEquals(
            "https://example.com/item",
            UrlUtils.extractUrlFromText("See: https://example.com/item;")
        )
    }

    @Test
    fun extractUrl_urlInMiddleOfParagraph() {
        val sharedText = "Header text\nSome comments https://blog.jetbrains.com/kotlin/2024/whats-new/ and trailing thoughts"
        val extracted = UrlUtils.extractUrlFromText(sharedText)
        assertEquals("https://blog.jetbrains.com/kotlin/2024/whats-new", extracted)
    }

    @Test
    fun extractUrl_rejectsInvalidAndUnsupportedSchemes() {
        assertNull(UrlUtils.extractUrlFromText("No url here just plain text"))
        assertNull(UrlUtils.extractUrlFromText("file:///sdcard/document.pdf"))
        assertNull(UrlUtils.extractUrlFromText("content://media/external/images/123"))
        assertNull(UrlUtils.extractUrlFromText("javascript:alert(1)"))
        assertNull(UrlUtils.extractUrlFromText("ftp://files.example.com/download.zip"))
        assertNull(UrlUtils.extractUrlFromText(null))
        assertNull(UrlUtils.extractUrlFromText(""))
        assertNull(UrlUtils.extractUrlFromText("     "))
    }
}
