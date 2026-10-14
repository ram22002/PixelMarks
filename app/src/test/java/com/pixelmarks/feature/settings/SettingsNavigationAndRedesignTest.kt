package com.pixelmarks.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.app.navigation.Screen
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.security.biometric.BiometricAuthManager
import com.pixelmarks.data.repository.AppColorScheme
import com.pixelmarks.data.repository.AppPreferencesRepositoryImpl
import com.pixelmarks.data.repository.BrowserPreferenceRepositoryImpl
import com.pixelmarks.data.repository.CollectionStyle
import com.pixelmarks.data.repository.DisplayFont
import com.pixelmarks.data.repository.PrivatePreferencesRepositoryImpl
import com.pixelmarks.data.repository.SecurityPreferencesRepositoryImpl
import com.pixelmarks.data.repository.StartupPage
import com.pixelmarks.data.repository.ThemeMode
import com.pixelmarks.domain.usecase.CheckBiometricCapabilityUseCase
import com.pixelmarks.domain.usecase.ObserveAppLockSettingsUseCase
import com.pixelmarks.domain.usecase.ObserveBrowserPreferenceUseCase
import com.pixelmarks.domain.usecase.SetAppLockEnabledUseCase
import com.pixelmarks.domain.usecase.SetAppLockTimeoutUseCase
import com.pixelmarks.domain.usecase.SetBrowserPreferenceUseCase
import androidx.datastore.preferences.core.edit
import com.pixelmarks.data.datastore.userPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsNavigationAndRedesignTest {

    private lateinit var context: Context
    private lateinit var browserRepo: BrowserPreferenceRepositoryImpl
    private lateinit var appPrefsRepo: AppPreferencesRepositoryImpl
    private lateinit var securityRepo: SecurityPreferencesRepositoryImpl
    private lateinit var privatePrefsRepo: PrivatePreferencesRepositoryImpl
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.userPreferencesDataStore.edit { it.clear() }
        browserRepo = BrowserPreferenceRepositoryImpl(context)
        appPrefsRepo = AppPreferencesRepositoryImpl(context)
        securityRepo = SecurityPreferencesRepositoryImpl(context)
        privatePrefsRepo = PrivatePreferencesRepositoryImpl(context)

        val biometricManager = BiometricAuthManager(context)
        val appLockManager = com.pixelmarks.core.security.AppLockManager(securityRepo)

        viewModel = SettingsViewModel(
            observeBrowserPreferenceUseCase = ObserveBrowserPreferenceUseCase(browserRepo),
            setBrowserPreferenceUseCase = SetBrowserPreferenceUseCase(browserRepo),
            observeAppLockSettingsUseCase = ObserveAppLockSettingsUseCase(securityRepo),
            setAppLockEnabledUseCase = SetAppLockEnabledUseCase(securityRepo, appLockManager),
            setAppLockTimeoutUseCase = SetAppLockTimeoutUseCase(securityRepo),
            checkBiometricCapabilityUseCase = CheckBiometricCapabilityUseCase(biometricManager),
            biometricAuthManager = biometricManager,
            privatePreferencesRepository = privatePrefsRepo,
            appPreferencesRepository = appPrefsRepo
        )
    }

    @Test
    fun browserPreference_selectionUpdatesPersistenceAndViewModel() = runBlocking {
        // Initial / Default
        assertEquals(BrowserPreference.SYSTEM_PREFERRED, browserRepo.getPreference())

        // Change to IN_APP
        viewModel.setBrowserPreference(BrowserPreference.IN_APP)
        assertEquals(BrowserPreference.IN_APP, browserRepo.getPreference())
        assertEquals(BrowserPreference.IN_APP, browserRepo.observePreference().first())

        // Change to DEFAULT_BROWSER
        viewModel.setBrowserPreference(BrowserPreference.DEFAULT_BROWSER)
        assertEquals(BrowserPreference.DEFAULT_BROWSER, browserRepo.getPreference())

        // Change to ASK_EVERY_TIME
        viewModel.setBrowserPreference(BrowserPreference.ASK_EVERY_TIME)
        assertEquals(BrowserPreference.ASK_EVERY_TIME, browserRepo.getPreference())
    }

    @Test
    fun settingsNavigation_dedicatedDetailRoutesAreDistinctAndAccessible() {
        assertEquals("settings/browser_preference", Screen.BrowserPreferenceSetting.route)
        assertEquals("settings/app_lock", Screen.AppLockSetting.route)
        assertEquals("settings/secure_folder", Screen.SecureFolderSetting.route)
        assertEquals("settings/security", Screen.SecuritySettings.route)
        assertEquals("settings/customize_bookmark", Screen.CustomizeBookmark.route)

        // Verify distinct from root settings
        assertNotEquals(Screen.Settings.route, Screen.BrowserPreferenceSetting.route)
        assertNotEquals(Screen.Settings.route, Screen.AppLockSetting.route)
        assertNotEquals(Screen.Settings.route, Screen.SecureFolderSetting.route)
        assertNotEquals(Screen.Settings.route, Screen.SecuritySettings.route)
        assertNotEquals(Screen.Settings.route, Screen.CustomizeBookmark.route)

        // Verify App Lock and Secure Folder are separate routes
        assertNotEquals(Screen.AppLockSetting.route, Screen.SecureFolderSetting.route)

        // Existing data management routes
        assertEquals("backup", Screen.Backup.route)
        assertEquals("connect_pc", Screen.ConnectPc.route)
        assertEquals("connected_devices", Screen.ConnectedDevices.route)
        assertEquals("html_import_export", Screen.HtmlImportExport.route)
        assertEquals("tags", Screen.Tags.route)
    }

    @Test
    fun themeAndAppearance_settingsViewModelUpdatesFlows() = runBlocking {
        // Theme mode
        viewModel.updateThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, appPrefsRepo.observeThemeMode().first())

        // Dynamic theme
        viewModel.updateDynamicTheme(true)
        assertTrue(appPrefsRepo.observeDynamicTheme().first())

        // Color scheme
        viewModel.updateColorScheme(AppColorScheme.OBSIDIAN)
        assertEquals(AppColorScheme.OBSIDIAN, appPrefsRepo.observeColorScheme().first())

        // Display font
        viewModel.updateDisplayFont(DisplayFont.TECHNICAL_MONO)
        assertEquals(DisplayFont.TECHNICAL_MONO, appPrefsRepo.observeDisplayFont().first())

        // Language
        viewModel.updateLanguage("English")
        assertEquals("English", appPrefsRepo.observeLanguage().first())

        // Startup page
        viewModel.updateStartupPage(StartupPage.TAGS)
        assertEquals(StartupPage.TAGS, appPrefsRepo.observeStartupPage().first())

        // Collection style
        viewModel.updateCollectionStyle(CollectionStyle.GRID)
        assertEquals(CollectionStyle.GRID, appPrefsRepo.observeCollectionStyle().first())
    }
}
