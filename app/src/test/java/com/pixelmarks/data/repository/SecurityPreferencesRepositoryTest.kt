package com.pixelmarks.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.security.model.AppLockTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SecurityPreferencesRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        repository = SecurityPreferencesRepositoryImpl(context)
        repository.setAppLockEnabled(false)
        repository.setLockTimeout(AppLockTimeout.IMMEDIATELY)
    }

    @Test
    fun appLock_defaultIsDisabled() = runBlocking {
        assertFalse(repository.isAppLockEnabled())
        assertFalse(repository.observeAppLockEnabled().first())
    }

    @Test
    fun appLock_enableAndDisable() = runBlocking {
        repository.setAppLockEnabled(true)
        assertTrue(repository.isAppLockEnabled())
        assertTrue(repository.observeAppLockEnabled().first())

        repository.setAppLockEnabled(false)
        assertFalse(repository.isAppLockEnabled())
        assertFalse(repository.observeAppLockEnabled().first())
    }

    @Test
    fun lockTimeout_defaultIsImmediately() = runBlocking {
        assertEquals(AppLockTimeout.IMMEDIATELY, repository.getLockTimeout())
        assertEquals(AppLockTimeout.IMMEDIATELY, repository.observeLockTimeout().first())
    }

    @Test
    fun lockTimeout_persistAndObserveDifferentValues() = runBlocking {
        repository.setLockTimeout(AppLockTimeout.FIVE_MINUTES)
        assertEquals(AppLockTimeout.FIVE_MINUTES, repository.getLockTimeout())
        assertEquals(AppLockTimeout.FIVE_MINUTES, repository.observeLockTimeout().first())

        repository.setLockTimeout(AppLockTimeout.FIFTEEN_MINUTES)
        assertEquals(AppLockTimeout.FIFTEEN_MINUTES, repository.getLockTimeout())
        assertEquals(AppLockTimeout.FIFTEEN_MINUTES, repository.observeLockTimeout().first())

        repository.setLockTimeout(AppLockTimeout.ONE_MINUTE)
        assertEquals(AppLockTimeout.ONE_MINUTE, repository.getLockTimeout())
        assertEquals(AppLockTimeout.ONE_MINUTE, repository.observeLockTimeout().first())
    }
}
