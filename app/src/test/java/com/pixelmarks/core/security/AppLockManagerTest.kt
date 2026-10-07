package com.pixelmarks.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.security.model.AppLockTimeout
import com.pixelmarks.data.repository.SecurityPreferencesRepository
import com.pixelmarks.data.repository.SecurityPreferencesRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppLockManagerTest {

    private lateinit var context: Context
    private lateinit var repository: SecurityPreferencesRepository
    private lateinit var job: Job
    private lateinit var managerScope: CoroutineScope

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        repository = SecurityPreferencesRepositoryImpl(context)
        repository.setAppLockEnabled(false)
        repository.setLockTimeout(AppLockTimeout.IMMEDIATELY)
        job = Job()
        managerScope = CoroutineScope(Dispatchers.Unconfined + job)
    }

    @After
    fun tearDown() {
        job.cancel()
    }

    @Test
    fun appLock_disabled_doesNotLockOnForeground() = runBlocking {
        repository.setAppLockEnabled(false)
        val manager = AppLockManager(repository, managerScope)

        manager.onAppBackgrounded()
        manager.onAppForegrounded()

        assertFalse(manager.isAppLocked.value)
    }

    @Test
    fun appLock_enabled_immediately_locksOnForeground() = runBlocking {
        repository.setAppLockEnabled(true)
        repository.setLockTimeout(AppLockTimeout.IMMEDIATELY)
        val manager = AppLockManager(repository, managerScope)

        manager.unlockApp()
        assertFalse(manager.isAppLocked.value)

        manager.onAppBackgrounded()
        manager.onAppForegrounded()

        assertTrue(manager.isAppLocked.value)
    }

    @Test
    fun appLock_unlockAndLock() = runBlocking {
        repository.setAppLockEnabled(true)
        val manager = AppLockManager(repository, managerScope)

        manager.lockApp()
        assertTrue(manager.isAppLocked.value)

        manager.unlockApp()
        assertFalse(manager.isAppLocked.value)
    }

    @Test
    fun privateSession_unlockAndLock() = runBlocking {
        val manager = AppLockManager(repository, managerScope)

        assertFalse(manager.isPrivateSessionUnlocked.value)

        manager.unlockPrivateSession()
        assertTrue(manager.isPrivateSessionUnlocked.value)

        manager.lockPrivateSession()
        assertFalse(manager.isPrivateSessionUnlocked.value)
    }

    @Test
    fun collectionUnlock_individualAndSession() = runBlocking {
        val manager = AppLockManager(repository, managerScope)

        assertFalse(manager.isCollectionUnlocked(42L))

        manager.unlockCollection(42L)
        assertTrue(manager.isCollectionUnlocked(42L))
        assertFalse(manager.isCollectionUnlocked(99L))

        // Private session automatically unlocks all collections
        manager.unlockPrivateSession()
        assertTrue(manager.isCollectionUnlocked(99L))
    }
}
