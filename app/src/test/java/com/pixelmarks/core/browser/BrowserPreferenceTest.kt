package com.pixelmarks.core.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class BrowserPreferenceTest {

    @Test
    fun browserPreference_defaultValueIsSystemPreferred() {
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, BrowserPreference.DEFAULT)
    }

    @Test
    fun browserPreference_serializationUsesStandardNames() {
        assertEquals("IN_APP", BrowserPreference.IN_APP.name)
        assertEquals("SYSTEM_PREFERRED", BrowserPreference.SYSTEM_PREFERRED.name)
        assertEquals("DEFAULT_BROWSER", BrowserPreference.DEFAULT_BROWSER.name)
        assertEquals("ASK_EVERY_TIME", BrowserPreference.ASK_EVERY_TIME.name)
    }

    @Test
    fun browserPreference_deserializationParsesAllValidValues() {
        assertEquals(BrowserPreference.IN_APP, BrowserPreference.fromString("IN_APP"))
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, BrowserPreference.fromString("SYSTEM_PREFERRED"))
        assertEquals(BrowserPreference.DEFAULT_BROWSER, BrowserPreference.fromString("DEFAULT_BROWSER"))
        assertEquals(BrowserPreference.ASK_EVERY_TIME, BrowserPreference.fromString("ASK_EVERY_TIME"))
    }

    @Test
    fun browserPreference_deserializationHandlesWhitespace() {
        assertEquals(BrowserPreference.IN_APP, BrowserPreference.fromString("  IN_APP  "))
        assertEquals(BrowserPreference.DEFAULT_BROWSER, BrowserPreference.fromString("\nDEFAULT_BROWSER\t"))
    }

    @Test
    fun browserPreference_unknownOrCorruptValueFallsBackToSystemPreferred() {
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, BrowserPreference.fromString("CHROME_CUSTOM_TAB"))
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, BrowserPreference.fromString("UNKNOWN_GARBAGE"))
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, BrowserPreference.fromString(""))
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, BrowserPreference.fromString(null))
    }
}
