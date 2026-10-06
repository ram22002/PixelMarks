package com.pixelmarks.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun testValidHttpsUrl() {
        assertTrue(UrlUtils.isValidUrl("https://example.com"))
        assertTrue(UrlUtils.isValidUrl("https://example.com/path?param=1#fragment"))
    }

    @Test
    fun testValidHttpUrl() {
        assertTrue(UrlUtils.isValidUrl("http://example.com"))
        assertTrue(UrlUtils.isValidUrl("http://example.org/test/page"))
    }

    @Test
    fun testUrlWithoutScheme() {
        assertTrue(UrlUtils.isValidUrl("example.com"))
        assertTrue(UrlUtils.isValidUrl("subdomain.example.com/path"))
    }

    @Test
    fun testBlankUrl() {
        assertFalse(UrlUtils.isValidUrl(""))
        assertFalse(UrlUtils.isValidUrl("   "))
    }

    @Test
    fun testInvalidUrl() {
        assertFalse(UrlUtils.isValidUrl("hello"))
        assertFalse(UrlUtils.isValidUrl("not_a_valid_url"))
        assertFalse(UrlUtils.isValidUrl("ftp://test.com"))
    }

    @Test
    fun testNormalizationTrailingSlash() {
        assertEquals("https://example.com", UrlUtils.normalizeUrl("https://example.com/"))
        assertEquals("https://example.com/path", UrlUtils.normalizeUrl("https://example.com/path/"))
    }

    @Test
    fun testNormalizationCasingAndWhitespace() {
        assertEquals(
            "https://example.com/path?query=val",
            UrlUtils.normalizeUrl("   HTTPS://Example.COM/path?query=val   ")
        )
    }

    @Test
    fun testNormalizationMissingScheme() {
        assertEquals(
            "https://example.com/article",
            UrlUtils.normalizeUrl("example.com/article")
        )
    }

    @Test
    fun testDomainExtraction() {
        assertEquals("example.com", UrlUtils.extractDomain("https://example.com/page"))
        assertEquals("example.com", UrlUtils.extractDomain("https://www.example.com/page"))
        assertEquals("github.com", UrlUtils.extractDomain("github.com/kotlin/kotlin"))
        assertEquals("sub.domain.org", UrlUtils.extractDomain("https://sub.domain.org:8080/"))
    }
}
