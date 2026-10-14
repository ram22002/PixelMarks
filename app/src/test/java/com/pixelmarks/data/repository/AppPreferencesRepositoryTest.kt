package com.pixelmarks.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import com.pixelmarks.data.datastore.userPreferencesDataStore
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
class AppPreferencesRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AppPreferencesRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.userPreferencesDataStore.edit { it.clear() }
        repository = AppPreferencesRepositoryImpl(context)
    }

    @Test
    fun appPreferencesRepository_defaultsToWarmArchivalSettings() = runBlocking {
        assertEquals(ThemeMode.DARK, repository.observeThemeMode().first())
        assertFalse(repository.observeDynamicTheme().first())
        assertEquals(AppColorScheme.ARCHIVAL_GOLD, repository.observeColorScheme().first())
        assertEquals(DisplayFont.EDITORIAL_SERIF, repository.observeDisplayFont().first())
        assertEquals("English", repository.observeLanguage().first())
        assertEquals(StartupPage.HOME, repository.observeStartupPage().first())
        assertEquals(CollectionStyle.FOLIO, repository.observeCollectionStyle().first())
        assertTrue(repository.observeShowFavicon().first())
        assertTrue(repository.observeShowDomain().first())
        assertTrue(repository.observeShowNotes().first())
        assertFalse(repository.observeCompactMode().first())
    }

    @Test
    fun appPreferencesRepository_updatesAndPersistsThemeAndAppearance() = runBlocking {
        // Theme Mode
        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.observeThemeMode().first())

        repository.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repository.observeThemeMode().first())

        // Dynamic Theme
        repository.setDynamicTheme(true)
        assertTrue(repository.observeDynamicTheme().first())

        // Color Scheme
        repository.setColorScheme(AppColorScheme.TERRACOTTA)
        assertEquals(AppColorScheme.TERRACOTTA, repository.observeColorScheme().first())

        repository.setColorScheme(AppColorScheme.EMERALD)
        assertEquals(AppColorScheme.EMERALD, repository.observeColorScheme().first())

        // Display Font
        repository.setDisplayFont(DisplayFont.TECHNICAL_MONO)
        assertEquals(DisplayFont.TECHNICAL_MONO, repository.observeDisplayFont().first())

        // Startup Page
        repository.setStartupPage(StartupPage.COLLECTIONS)
        assertEquals(StartupPage.COLLECTIONS, repository.observeStartupPage().first())

        // Collection Style
        repository.setCollectionStyle(CollectionStyle.GRID)
        assertEquals(CollectionStyle.GRID, repository.observeCollectionStyle().first())

        // Bookmark card custom toggles
        repository.setShowFavicon(false)
        assertFalse(repository.observeShowFavicon().first())

        repository.setCompactMode(true)
        assertTrue(repository.observeCompactMode().first())
    }
}
