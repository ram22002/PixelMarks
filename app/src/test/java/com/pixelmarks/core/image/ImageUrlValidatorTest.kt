package com.pixelmarks.core.image

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUrlValidatorTest {

    @Test
    fun isValidImageUrl_httpsUrl_accepted() {
        assertTrue(ImageUrlValidator.isValidImageUrl("https://example.com/images/card.jpg"))
    }

    @Test
    fun isValidImageUrl_httpUrl_accepted() {
        assertTrue(ImageUrlValidator.isValidImageUrl("http://example.com/favicon.png"))
    }

    @Test
    fun isValidImageUrl_fileUrl_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl("file:///sdcard/Download/secret.png"))
    }

    @Test
    fun isValidImageUrl_contentUrl_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl("content://media/external/images/media/1"))
    }

    @Test
    fun isValidImageUrl_dataUrl_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."))
    }

    @Test
    fun isValidImageUrl_javascriptUrl_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl("javascript:alert('malicious')"))
    }

    @Test
    fun isValidImageUrl_intentUrl_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl("intent://something#Intent;scheme=http;package=com.hack;end"))
    }

    @Test
    fun isValidImageUrl_nullOrBlank_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl(null))
        assertFalse(ImageUrlValidator.isValidImageUrl(""))
        assertFalse(ImageUrlValidator.isValidImageUrl("   "))
    }

    @Test
    fun isValidImageUrl_urlWithSpaces_rejected() {
        assertFalse(ImageUrlValidator.isValidImageUrl("https://example.com/invalid path.png"))
    }
}
