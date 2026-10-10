package com.pixelmarks.feature.backup

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.backup.crypto.AutoBackupKeyManager
import com.pixelmarks.core.backup.model.AutoBackupFrequency
import com.pixelmarks.core.backup.model.AutoBackupPreferences
import com.pixelmarks.core.backup.model.DriveBackupFile
import com.pixelmarks.core.backup.model.DriveConnectionState
import com.pixelmarks.core.backup.model.GoogleAccountInfo
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.data.repository.BackupRepositoryImpl
import com.pixelmarks.data.repository.BrowserPreferenceRepositoryImpl
import com.pixelmarks.data.repository.DriveBackupRepositoryImpl
import com.pixelmarks.data.repository.DrivePreferencesRepository
import com.pixelmarks.data.repository.DrivePreferencesRepositoryImpl
import com.pixelmarks.data.repository.FakeGoogleAuthManager
import com.pixelmarks.data.repository.FakeGoogleDriveClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.pixelmarks.domain.usecase.ConnectDriveUseCase
import com.pixelmarks.domain.usecase.CreateBackupUseCase
import com.pixelmarks.domain.usecase.DeleteDriveBackupUseCase
import com.pixelmarks.domain.usecase.DisconnectDriveUseCase
import com.pixelmarks.domain.usecase.DownloadAndInspectDriveBackupUseCase
import com.pixelmarks.domain.usecase.InspectBackupUseCase
import com.pixelmarks.domain.usecase.ListDriveBackupsUseCase
import com.pixelmarks.domain.usecase.ObserveAutoBackupPreferencesUseCase
import com.pixelmarks.domain.usecase.ObserveDriveConnectionUseCase
import com.pixelmarks.domain.usecase.ObserveLatestBackupUseCase
import com.pixelmarks.domain.usecase.RestoreBackupUseCase
import com.pixelmarks.domain.usecase.RestoreDriveBackupUseCase
import com.pixelmarks.domain.usecase.UpdateAutoBackupPreferencesUseCase
import com.pixelmarks.domain.usecase.UploadDriveBackupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeDrivePreferencesRepository : DrivePreferencesRepository {
    private val _prefs = MutableStateFlow(AutoBackupPreferences())
    override val autoBackupPreferences: Flow<AutoBackupPreferences> = _prefs

    private val _email = MutableStateFlow<String?>(null)
    override val connectedAccountEmail: Flow<String?> = _email

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        _prefs.value = _prefs.value.copy(enabled = enabled)
    }

    override suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency) {
        _prefs.value = _prefs.value.copy(frequency = frequency)
    }

    override suspend fun setWifiOnly(wifiOnly: Boolean) {
        _prefs.value = _prefs.value.copy(wifiOnly = wifiOnly)
    }

    override suspend fun updateLastCloudBackup(time: Long, name: String) {
        _prefs.value = _prefs.value.copy(lastAutoBackupTime = time)
    }

    override suspend fun setConnectedAccountEmail(email: String?) {
        _email.value = email
    }

    override suspend fun clear() {
        _prefs.value = AutoBackupPreferences()
        _email.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DriveBackupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: PixelMarksDatabase
    private lateinit var fakeAuthManager: FakeGoogleAuthManager
    private lateinit var fakeDriveClient: FakeGoogleDriveClient
    private lateinit var fakeDrivePrefsRepo: FakeDrivePreferencesRepository
    private lateinit var viewModel: BackupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(context)

        database = PixelMarksDatabase.buildInMemory(context)
        val browserRepo = BrowserPreferenceRepositoryImpl(context)
        val backupRepo = BackupRepositoryImpl(context, database, browserRepo, testDispatcher)
        fakeDrivePrefsRepo = FakeDrivePreferencesRepository()
        val keyManager = AutoBackupKeyManager(context)
        keyManager.clearAutoBackupPassword()

        fakeAuthManager = FakeGoogleAuthManager()
        fakeDriveClient = FakeGoogleDriveClient()

        val driveBackupRepo = DriveBackupRepositoryImpl(
            context = context,
            database = database,
            backupRepository = backupRepo,
            googleAuthManager = fakeAuthManager,
            googleDriveClient = fakeDriveClient,
            drivePreferencesRepository = fakeDrivePrefsRepo,
            autoBackupKeyManager = keyManager,
            ioDispatcher = testDispatcher
        )

        viewModel = BackupViewModel(
            createBackupUseCase = CreateBackupUseCase(backupRepo),
            inspectBackupUseCase = InspectBackupUseCase(backupRepo),
            restoreBackupUseCase = RestoreBackupUseCase(backupRepo),
            observeLatestBackupUseCase = ObserveLatestBackupUseCase(backupRepo),
            observeDriveConnectionUseCase = ObserveDriveConnectionUseCase(driveBackupRepo),
            connectDriveUseCase = ConnectDriveUseCase(driveBackupRepo),
            disconnectDriveUseCase = DisconnectDriveUseCase(driveBackupRepo),
            listDriveBackupsUseCase = ListDriveBackupsUseCase(driveBackupRepo),
            uploadDriveBackupUseCase = UploadDriveBackupUseCase(driveBackupRepo),
            downloadAndInspectDriveBackupUseCase = DownloadAndInspectDriveBackupUseCase(driveBackupRepo),
            restoreDriveBackupUseCase = RestoreDriveBackupUseCase(driveBackupRepo),
            deleteDriveBackupUseCase = DeleteDriveBackupUseCase(driveBackupRepo),
            observeAutoBackupPreferencesUseCase = ObserveAutoBackupPreferencesUseCase(driveBackupRepo),
            updateAutoBackupPreferencesUseCase = UpdateAutoBackupPreferencesUseCase(driveBackupRepo)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun initialState_isDisconnected() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(DriveConnectionState.DISCONNECTED, state.driveConnectionState)
        assertNull(state.driveConnectedAccount)
        assertTrue(state.driveBackups.isEmpty())
    }

    @Test
    fun handleGoogleSignInResult_updatesStateToConnected() = runTest {
        viewModel.handleGoogleSignInResult(Intent())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DriveConnectionState.CONNECTED, state.driveConnectionState)
        assertEquals("user@example.com", state.driveConnectedAccount?.email)
    }

    @Test
    fun disconnect_clearsConnectedAccountAndBackups() = runTest {
        viewModel.handleGoogleSignInResult(Intent())
        advanceUntilIdle()
        assertEquals(DriveConnectionState.CONNECTED, viewModel.uiState.value.driveConnectionState)

        viewModel.disconnectGoogleDrive()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DriveConnectionState.DISCONNECTED, state.driveConnectionState)
        assertNull(state.driveConnectedAccount)
        assertTrue(state.driveBackups.isEmpty())
    }

    @Test
    fun uploadToDrive_and_refreshBackups() = runTest {
        viewModel.handleGoogleSignInResult(Intent())
        advanceUntilIdle()

        viewModel.uploadBackupToDrive("CloudPassword123!".toCharArray())
        var state = viewModel.uiState.value
        val startTime = System.currentTimeMillis()
        while (state.cloudUploadSuccess == null && state.errorMessage == null && System.currentTimeMillis() - startTime < 3000) {
            testScheduler.advanceTimeBy(100)
            Thread.sleep(50)
            state = viewModel.uiState.value
        }

        assertNull(state.errorMessage)
        assertNotNull(state.cloudUploadSuccess)
        assertEquals(1, state.driveBackups.size)

        viewModel.dismissSuccessDialog()
        assertNull(viewModel.uiState.value.cloudUploadSuccess)
    }

    @Test
    fun deleteBackupDialog_flow() = runTest {
        val backupFile = DriveBackupFile("id_1", "test.pmb", 1024, 1000, 1000)
        viewModel.requestDeleteDriveBackup(backupFile)
        assertEquals(backupFile, viewModel.uiState.value.showDeleteDriveBackupConfirm)

        viewModel.dismissDeleteDriveBackup()
        assertNull(viewModel.uiState.value.showDeleteDriveBackupConfirm)
    }

    @Test
    fun autoBackupPreferences_updates() = runTest {
        viewModel.setAutoBackupFrequency(AutoBackupFrequency.WEEKLY)
        viewModel.setWifiOnly(false)
        advanceUntilIdle()

        val prefs = viewModel.uiState.value.autoBackupPreferences
        assertEquals(AutoBackupFrequency.WEEKLY, prefs.frequency)
        assertFalse(prefs.wifiOnly)
    }
}
