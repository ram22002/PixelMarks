package com.pixelmarks.core.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebUrlValidatorTest {

    @Test
    fun webUrlValidator_acceptsValidHttpAndHttps() {
        assertTrue(WebUrlValidator.isValidWebUrl("http://example.com"))
        assertTrue(WebUrlValidator.isValidWebUrl("https://example.com"))
        assertTrue(WebUrlValidator.isValidWebUrl("https://news.ycombinator.com/item?id=12345"))
        assertTrue(WebUrlValidator.isValidWebUrl("http://192.168.1.1:8080/path"))
    }

    @Test
    fun webUrlValidator_trimsWhitespaceInSanitize() {
        assertEquals("https://example.com", WebUrlValidator.sanitizeUrl("   https://example.com  \n"))
    }

    @Test
    fun webUrlValidator_rejectsDangerousSchemes() {
        assertFalse(WebUrlValidator.isValidWebUrl("javascript:alert(1)"))
        assertFalse(WebUrlValidator.isValidWebUrl("file:///android_asset/secret.txt"))
        assertFalse(WebUrlValidator.isValidWebUrl("content://com.android.providers.media/documents"))
        assertFalse(WebUrlValidator.isValidWebUrl("data:text/html,<html>"))
        assertFalse(WebUrlValidator.isValidWebUrl("intent:#Intent;action=android.intent.action.VIEW;end"))
        assertFalse(WebUrlValidator.isValidWebUrl("chrome://version"))
        assertFalse(WebUrlValidator.isValidWebUrl("about:blank"))
    }

    @Test
    fun webUrlValidator_rejectsUnknownSchemes() {
        assertFalse(WebUrlValidator.isValidWebUrl("ftp://ftp.example.com"))
        assertFalse(WebUrlValidator.isValidWebUrl("ssh://server.com"))
        assertFalse(WebUrlValidator.isValidWebUrl("smb://nas/share"))
        assertFalse(WebUrlValidator.isValidWebUrl("customapp://open"))
    }

    @Test
    fun webUrlValidator_rejectsNullEmptyAndMalformed() {
        assertFalse(WebUrlValidator.isValidWebUrl(null))
        assertFalse(WebUrlValidator.isValidWebUrl(""))
        assertFalse(WebUrlValidator.isValidWebUrl("   "))
        assertFalse(WebUrlValidator.isValidWebUrl("https:// invalid space .com"))
        assertFalse(WebUrlValidator.isValidWebUrl("http://"))
    }
}
