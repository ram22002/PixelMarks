package com.pixelmarks.core.security

import com.pixelmarks.core.security.model.AppLockTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockTimeoutTest {

    @Test
    fun timeout_durationMillis_areAccurate() {
        assertEquals(0L, AppLockTimeout.IMMEDIATELY.durationMillis)
        assertEquals(60_000L, AppLockTimeout.ONE_MINUTE.durationMillis)
        assertEquals(300_000L, AppLockTimeout.FIVE_MINUTES.durationMillis)
        assertEquals(900_000L, AppLockTimeout.FIFTEEN_MINUTES.durationMillis)
    }

    @Test
    fun timeout_fromString_parsesValidValues() {
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromString("IMMEDIATELY"))
        assertEquals(AppLockTimeout.ONE_MINUTE, AppLockTimeout.fromString("ONE_MINUTE"))
        assertEquals(AppLockTimeout.FIVE_MINUTES, AppLockTimeout.fromString("FIVE_MINUTES"))
        assertEquals(AppLockTimeout.FIFTEEN_MINUTES, AppLockTimeout.fromString("FIFTEEN_MINUTES"))
    }

    @Test
    fun timeout_fromString_fallsBackSafelyOnInvalidOrNull() {
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromString(null))
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromString(""))
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromString("UNKNOWN_VALUE"))
        assertEquals(AppLockTimeout.IMMEDIATELY, AppLockTimeout.fromString("30_SECONDS"))
    }

    @Test
    fun timeout_displayLabels_areNonEmpty() {
        for (timeout in AppLockTimeout.entries) {
            assert(timeout.label.isNotBlank())
        }
    }
}
