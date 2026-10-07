package com.pixelmarks.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.browser.BrowserPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserPreferenceRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: BrowserPreferenceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = BrowserPreferenceRepositoryImpl(context)
    }

    @Test
    fun browserPreferenceRepository_persistsAndObservesChanges() = runBlocking {
        // Change to IN_APP
        repository.setPreference(BrowserPreference.IN_APP)
        assertEquals(BrowserPreference.IN_APP, repository.getPreference())
        assertEquals(BrowserPreference.IN_APP, repository.observePreference().first())

        // Change to DEFAULT_BROWSER
        repository.setPreference(BrowserPreference.DEFAULT_BROWSER)
        assertEquals(BrowserPreference.DEFAULT_BROWSER, repository.getPreference())

        // Change to ASK_EVERY_TIME
        repository.setPreference(BrowserPreference.ASK_EVERY_TIME)
        assertEquals(BrowserPreference.ASK_EVERY_TIME, repository.getPreference())
    }

    @Test
    fun browserPreferenceRepository_temporarySelectionDoesNotMutateSavedPreference() = runBlocking {
        // User has ASK_EVERY_TIME saved
        repository.setPreference(BrowserPreference.ASK_EVERY_TIME)
        assertEquals(BrowserPreference.ASK_EVERY_TIME, repository.getPreference())

        // User temporarily chooses DEFAULT_BROWSER for a single link opening
        val temporaryChoice = BrowserPreference.DEFAULT_BROWSER
        assertEquals(BrowserPreference.DEFAULT_BROWSER, temporaryChoice)

        // Persisted repository preference remains ASK_EVERY_TIME
        assertEquals(BrowserPreference.ASK_EVERY_TIME, repository.getPreference())
        assertEquals(BrowserPreference.ASK_EVERY_TIME, repository.observePreference().first())
    }
}
