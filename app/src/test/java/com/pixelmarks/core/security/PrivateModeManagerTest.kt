package com.pixelmarks.core.security

import com.pixelmarks.core.security.model.PrivacyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateModeManagerTest {

    @Test
    fun `default mode is PUBLIC`() {
        val manager = PrivateModeManagerImpl()
        assertEquals(PrivacyMode.PUBLIC, manager.mode.value)
        assertFalse(manager.isPrivateActive())
    }

    @Test
    fun `enterPrivateMode sets mode to PRIVATE`() {
        val manager = PrivateModeManagerImpl()
        manager.enterPrivateMode()
        assertEquals(PrivacyMode.PRIVATE, manager.mode.value)
        assertTrue(manager.isPrivateActive())
    }

    @Test
    fun `exitPrivateMode sets mode to PUBLIC`() {
        val manager = PrivateModeManagerImpl()
        manager.enterPrivateMode()
        manager.exitPrivateMode()
        assertEquals(PrivacyMode.PUBLIC, manager.mode.value)
        assertFalse(manager.isPrivateActive())
    }

    @Test
    fun `onAppBackgrounded automatically resets mode to PUBLIC`() {
        val manager = PrivateModeManagerImpl()
        manager.enterPrivateMode()
        assertTrue(manager.isPrivateActive())

        manager.onAppBackgrounded()
        assertEquals(PrivacyMode.PUBLIC, manager.mode.value)
        assertFalse(manager.isPrivateActive())
    }
}
