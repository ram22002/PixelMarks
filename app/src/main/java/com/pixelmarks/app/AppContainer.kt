package com.pixelmarks.app

import android.content.Context
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.network.DefaultMetadataHttpClient
import com.pixelmarks.core.network.MetadataHttpClient
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.BookmarkRepositoryImpl
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.data.repository.CollectionRepositoryImpl
import com.pixelmarks.data.repository.MetadataRepository
import com.pixelmarks.data.repository.MetadataRepositoryImpl
import com.pixelmarks.data.repository.TagRepository
import com.pixelmarks.data.repository.TagRepositoryImpl
import com.pixelmarks.domain.usecase.AddBookmarkUseCase
import com.pixelmarks.domain.usecase.ArchiveBookmarkUseCase
import com.pixelmarks.domain.usecase.DeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.FetchBookmarkMetadataUseCase
import com.pixelmarks.domain.usecase.GetBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkByIdUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksUseCase
import com.pixelmarks.domain.usecase.ObserveTrashedBookmarksUseCase
import com.pixelmarks.domain.usecase.PermanentlyDeleteBookmarkUseCase
import com.pixelmarks.domain.usecase.RestoreBookmarkUseCase
import com.pixelmarks.domain.usecase.SearchBookmarksUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkFavoriteUseCase
import com.pixelmarks.domain.usecase.ToggleBookmarkPinUseCase
import com.pixelmarks.domain.usecase.TrashBookmarkUseCase
import com.pixelmarks.domain.usecase.UnarchiveBookmarkUseCase
import coil3.ImageLoader
import com.pixelmarks.core.image.PixelMarksImageLoader
import com.pixelmarks.domain.usecase.CreateCollectionUseCase
import com.pixelmarks.domain.usecase.DeleteCollectionUseCase
import com.pixelmarks.domain.usecase.GetCollectionUseCase
import com.pixelmarks.domain.usecase.MoveBookmarkUseCase
import com.pixelmarks.domain.usecase.ObserveCollectionsUseCase
import com.pixelmarks.domain.usecase.UpdateBookmarkUseCase
import com.pixelmarks.domain.usecase.UpdateCollectionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import com.pixelmarks.domain.usecase.CreateTagUseCase
import com.pixelmarks.domain.usecase.DeleteTagUseCase
import com.pixelmarks.domain.usecase.GetTagUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarksByTagUseCase
import com.pixelmarks.domain.usecase.ObserveTagsUseCase
import com.pixelmarks.domain.usecase.ReplaceBookmarkTagsUseCase
import com.pixelmarks.domain.usecase.UpdateTagUseCase
import com.pixelmarks.data.repository.ReminderRepository
import com.pixelmarks.data.repository.ReminderRepositoryImpl
import com.pixelmarks.domain.usecase.CancelBookmarkReminderUseCase
import com.pixelmarks.domain.usecase.GetBookmarkReminderUseCase
import com.pixelmarks.domain.usecase.MarkReminderCompletedUseCase
import com.pixelmarks.domain.usecase.ObserveBookmarkReminderUseCase
import com.pixelmarks.domain.usecase.ObserveUpcomingRemindersUseCase
import com.pixelmarks.domain.usecase.SetBookmarkReminderUseCase
import com.pixelmarks.data.repository.BrowserPreferenceRepository
import com.pixelmarks.data.repository.BrowserPreferenceRepositoryImpl
import com.pixelmarks.domain.usecase.GetBrowserPreferenceUseCase
import com.pixelmarks.domain.usecase.ObserveBrowserPreferenceUseCase
import com.pixelmarks.domain.usecase.RecordBookmarkVisitUseCase
import com.pixelmarks.domain.usecase.SetBrowserPreferenceUseCase
import com.pixelmarks.data.repository.BackupRepository
import com.pixelmarks.data.repository.BackupRepositoryImpl
import com.pixelmarks.domain.usecase.CreateBackupUseCase
import com.pixelmarks.domain.usecase.InspectBackupUseCase
import com.pixelmarks.domain.usecase.ObserveLatestBackupUseCase
import com.pixelmarks.domain.usecase.RestoreBackupUseCase
import com.pixelmarks.data.repository.SecurityPreferencesRepository
import com.pixelmarks.data.repository.SecurityPreferencesRepositoryImpl
import com.pixelmarks.core.security.biometric.BiometricAuthManager
import com.pixelmarks.core.security.AppLockManager
import com.pixelmarks.domain.usecase.ObserveAppLockSettingsUseCase
import com.pixelmarks.domain.usecase.SetAppLockEnabledUseCase
import com.pixelmarks.domain.usecase.SetAppLockTimeoutUseCase
import com.pixelmarks.domain.usecase.CheckBiometricCapabilityUseCase
import com.pixelmarks.domain.usecase.ManageAppLockUseCase

import com.pixelmarks.core.security.PrivateModeManager
import com.pixelmarks.core.security.PrivateModeManagerImpl
import com.pixelmarks.data.repository.PrivatePreferencesRepository
import com.pixelmarks.data.repository.PrivatePreferencesRepositoryImpl

/**
 * Application dependency container providing singleton instances of repositories and use cases.
 */
interface AppContainer {
    val privateModeManager: PrivateModeManager
    val privatePreferencesRepository: PrivatePreferencesRepository
    val bookmarkRepository: BookmarkRepository
    val collectionRepository: CollectionRepository
    val tagRepository: TagRepository
    val metadataRepository: MetadataRepository
    val imageLoader: ImageLoader

    val addBookmarkUseCase: AddBookmarkUseCase
    val observeBookmarksUseCase: ObserveBookmarksUseCase
    val observeBookmarkByIdUseCase: ObserveBookmarkByIdUseCase
    val searchBookmarksUseCase: SearchBookmarksUseCase
    val getBookmarkUseCase: GetBookmarkUseCase
    val deleteBookmarkUseCase: DeleteBookmarkUseCase
    val updateBookmarkUseCase: UpdateBookmarkUseCase
    val toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase
    val toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase
    val archiveBookmarkUseCase: ArchiveBookmarkUseCase
    val unarchiveBookmarkUseCase: UnarchiveBookmarkUseCase
    val trashBookmarkUseCase: TrashBookmarkUseCase
    val restoreBookmarkUseCase: RestoreBookmarkUseCase
    val permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase
    val observeTrashedBookmarksUseCase: ObserveTrashedBookmarksUseCase
    val fetchBookmarkMetadataUseCase: FetchBookmarkMetadataUseCase

    val observeCollectionsUseCase: ObserveCollectionsUseCase
    val getCollectionUseCase: GetCollectionUseCase
    val createCollectionUseCase: CreateCollectionUseCase
    val updateCollectionUseCase: UpdateCollectionUseCase
    val deleteCollectionUseCase: DeleteCollectionUseCase
    val moveBookmarkUseCase: MoveBookmarkUseCase

    val observeTagsUseCase: ObserveTagsUseCase
    val getTagUseCase: GetTagUseCase
    val createTagUseCase: CreateTagUseCase
    val updateTagUseCase: UpdateTagUseCase
    val deleteTagUseCase: DeleteTagUseCase
    val observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase
    val observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase
    val replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase

    val reminderRepository: ReminderRepository
    val setBookmarkReminderUseCase: SetBookmarkReminderUseCase
    val cancelBookmarkReminderUseCase: CancelBookmarkReminderUseCase
    val getBookmarkReminderUseCase: GetBookmarkReminderUseCase
    val observeBookmarkReminderUseCase: ObserveBookmarkReminderUseCase
    val observeUpcomingRemindersUseCase: ObserveUpcomingRemindersUseCase
    val markReminderCompletedUseCase: MarkReminderCompletedUseCase

    val browserPreferenceRepository: BrowserPreferenceRepository
    val observeBrowserPreferenceUseCase: ObserveBrowserPreferenceUseCase
    val setBrowserPreferenceUseCase: SetBrowserPreferenceUseCase
    val getBrowserPreferenceUseCase: GetBrowserPreferenceUseCase
    val recordBookmarkVisitUseCase: RecordBookmarkVisitUseCase

    val backupRepository: BackupRepository
    val createBackupUseCase: CreateBackupUseCase
    val inspectBackupUseCase: InspectBackupUseCase
    val restoreBackupUseCase: RestoreBackupUseCase
    val observeLatestBackupUseCase: ObserveLatestBackupUseCase

    val securityPreferencesRepository: SecurityPreferencesRepository
    val biometricAuthManager: BiometricAuthManager
    val appLockManager: AppLockManager
    val observeAppLockSettingsUseCase: ObserveAppLockSettingsUseCase
    val setAppLockEnabledUseCase: SetAppLockEnabledUseCase
    val setAppLockTimeoutUseCase: SetAppLockTimeoutUseCase
    val checkBiometricCapabilityUseCase: CheckBiometricCapabilityUseCase
    val manageAppLockUseCase: ManageAppLockUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: PixelMarksDatabase by lazy {
        PixelMarksDatabase.getInstance(context)
    }

    private val metadataHttpClient: MetadataHttpClient by lazy {
        DefaultMetadataHttpClient()
    }

    override val bookmarkRepository: BookmarkRepository by lazy {
        BookmarkRepositoryImpl(database.bookmarkDao())
    }

    override val collectionRepository: CollectionRepository by lazy {
        CollectionRepositoryImpl(database.collectionDao())
    }

    override val tagRepository: TagRepository by lazy {
        TagRepositoryImpl(database.tagDao(), database.bookmarkTagDao())
    }

    override val metadataRepository: MetadataRepository by lazy {
        MetadataRepositoryImpl(metadataHttpClient)
    }

    override val imageLoader: ImageLoader by lazy {
        PixelMarksImageLoader.create(context)
    }

    override val fetchBookmarkMetadataUseCase: FetchBookmarkMetadataUseCase by lazy {
        FetchBookmarkMetadataUseCase(bookmarkRepository, metadataRepository)
    }

    override val addBookmarkUseCase: AddBookmarkUseCase by lazy {
        AddBookmarkUseCase(
            bookmarkRepository = bookmarkRepository,
            fetchBookmarkMetadataUseCase = fetchBookmarkMetadataUseCase,
            tagRepository = tagRepository,
            reminderRepository = reminderRepository,
            collectionRepository = collectionRepository,
            coroutineScope = applicationScope
        )
    }

    override val observeBookmarksUseCase: ObserveBookmarksUseCase by lazy {
        ObserveBookmarksUseCase(bookmarkRepository)
    }

    override val observeBookmarkByIdUseCase: ObserveBookmarkByIdUseCase by lazy {
        ObserveBookmarkByIdUseCase(bookmarkRepository)
    }

    override val searchBookmarksUseCase: SearchBookmarksUseCase by lazy {
        SearchBookmarksUseCase(bookmarkRepository)
    }

    override val getBookmarkUseCase: GetBookmarkUseCase by lazy {
        GetBookmarkUseCase(bookmarkRepository)
    }

    override val deleteBookmarkUseCase: DeleteBookmarkUseCase by lazy {
        DeleteBookmarkUseCase(bookmarkRepository)
    }

    override val updateBookmarkUseCase: UpdateBookmarkUseCase by lazy {
        UpdateBookmarkUseCase(bookmarkRepository)
    }

    override val toggleBookmarkFavoriteUseCase: ToggleBookmarkFavoriteUseCase by lazy {
        ToggleBookmarkFavoriteUseCase(bookmarkRepository)
    }

    override val toggleBookmarkPinUseCase: ToggleBookmarkPinUseCase by lazy {
        ToggleBookmarkPinUseCase(bookmarkRepository)
    }

    override val archiveBookmarkUseCase: ArchiveBookmarkUseCase by lazy {
        ArchiveBookmarkUseCase(bookmarkRepository)
    }

    override val unarchiveBookmarkUseCase: UnarchiveBookmarkUseCase by lazy {
        UnarchiveBookmarkUseCase(bookmarkRepository)
    }

    override val trashBookmarkUseCase: TrashBookmarkUseCase by lazy {
        TrashBookmarkUseCase(bookmarkRepository)
    }

    override val restoreBookmarkUseCase: RestoreBookmarkUseCase by lazy {
        RestoreBookmarkUseCase(bookmarkRepository)
    }

    override val permanentlyDeleteBookmarkUseCase: PermanentlyDeleteBookmarkUseCase by lazy {
        PermanentlyDeleteBookmarkUseCase(bookmarkRepository)
    }

    override val observeTrashedBookmarksUseCase: ObserveTrashedBookmarksUseCase by lazy {
        ObserveTrashedBookmarksUseCase(bookmarkRepository)
    }

    override val observeCollectionsUseCase: ObserveCollectionsUseCase by lazy {
        ObserveCollectionsUseCase(collectionRepository)
    }

    override val getCollectionUseCase: GetCollectionUseCase by lazy {
        GetCollectionUseCase(collectionRepository)
    }

    override val createCollectionUseCase: CreateCollectionUseCase by lazy {
        CreateCollectionUseCase(collectionRepository)
    }

    override val updateCollectionUseCase: UpdateCollectionUseCase by lazy {
        UpdateCollectionUseCase(collectionRepository)
    }

    override val deleteCollectionUseCase: DeleteCollectionUseCase by lazy {
        DeleteCollectionUseCase(collectionRepository, bookmarkRepository)
    }

    override val moveBookmarkUseCase: MoveBookmarkUseCase by lazy {
        MoveBookmarkUseCase(bookmarkRepository, collectionRepository)
    }

    override val observeTagsUseCase: ObserveTagsUseCase by lazy {
        ObserveTagsUseCase(tagRepository)
    }

    override val getTagUseCase: GetTagUseCase by lazy {
        GetTagUseCase(tagRepository)
    }

    override val createTagUseCase: CreateTagUseCase by lazy {
        CreateTagUseCase(tagRepository)
    }

    override val updateTagUseCase: UpdateTagUseCase by lazy {
        UpdateTagUseCase(tagRepository)
    }

    override val deleteTagUseCase: DeleteTagUseCase by lazy {
        DeleteTagUseCase(tagRepository)
    }

    override val observeBookmarkTagsUseCase: ObserveBookmarkTagsUseCase by lazy {
        ObserveBookmarkTagsUseCase(tagRepository)
    }

    override val observeBookmarksByTagUseCase: ObserveBookmarksByTagUseCase by lazy {
        ObserveBookmarksByTagUseCase(tagRepository)
    }

    override val replaceBookmarkTagsUseCase: ReplaceBookmarkTagsUseCase by lazy {
        ReplaceBookmarkTagsUseCase(tagRepository)
    }

    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao(), bookmarkRepository, context)
    }

    override val setBookmarkReminderUseCase: SetBookmarkReminderUseCase by lazy {
        SetBookmarkReminderUseCase(reminderRepository)
    }

    override val cancelBookmarkReminderUseCase: CancelBookmarkReminderUseCase by lazy {
        CancelBookmarkReminderUseCase(reminderRepository)
    }

    override val getBookmarkReminderUseCase: GetBookmarkReminderUseCase by lazy {
        GetBookmarkReminderUseCase(reminderRepository)
    }

    override val observeBookmarkReminderUseCase: ObserveBookmarkReminderUseCase by lazy {
        ObserveBookmarkReminderUseCase(reminderRepository)
    }

    override val observeUpcomingRemindersUseCase: ObserveUpcomingRemindersUseCase by lazy {
        ObserveUpcomingRemindersUseCase(reminderRepository)
    }

    override val markReminderCompletedUseCase: MarkReminderCompletedUseCase by lazy {
        MarkReminderCompletedUseCase(reminderRepository)
    }

    override val browserPreferenceRepository: BrowserPreferenceRepository by lazy {
        BrowserPreferenceRepositoryImpl(context)
    }

    override val observeBrowserPreferenceUseCase: ObserveBrowserPreferenceUseCase by lazy {
        ObserveBrowserPreferenceUseCase(browserPreferenceRepository)
    }

    override val setBrowserPreferenceUseCase: SetBrowserPreferenceUseCase by lazy {
        SetBrowserPreferenceUseCase(browserPreferenceRepository)
    }

    override val getBrowserPreferenceUseCase: GetBrowserPreferenceUseCase by lazy {
        GetBrowserPreferenceUseCase(browserPreferenceRepository)
    }

    override val recordBookmarkVisitUseCase: RecordBookmarkVisitUseCase by lazy {
        RecordBookmarkVisitUseCase(bookmarkRepository)
    }

    override val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(context, database, browserPreferenceRepository)
    }

    override val createBackupUseCase: CreateBackupUseCase by lazy {
        CreateBackupUseCase(backupRepository)
    }

    override val inspectBackupUseCase: InspectBackupUseCase by lazy {
        InspectBackupUseCase(backupRepository)
    }

    override val restoreBackupUseCase: RestoreBackupUseCase by lazy {
        RestoreBackupUseCase(backupRepository)
    }

    override val observeLatestBackupUseCase: ObserveLatestBackupUseCase by lazy {
        ObserveLatestBackupUseCase(backupRepository)
    }

    override val securityPreferencesRepository: SecurityPreferencesRepository by lazy {
        SecurityPreferencesRepositoryImpl(context)
    }

    override val biometricAuthManager: BiometricAuthManager by lazy {
        BiometricAuthManager(context)
    }

    override val appLockManager: AppLockManager by lazy {
        AppLockManager(securityPreferencesRepository)
    }

    override val observeAppLockSettingsUseCase: ObserveAppLockSettingsUseCase by lazy {
        ObserveAppLockSettingsUseCase(securityPreferencesRepository)
    }

    override val setAppLockEnabledUseCase: SetAppLockEnabledUseCase by lazy {
        SetAppLockEnabledUseCase(securityPreferencesRepository, appLockManager)
    }

    override val setAppLockTimeoutUseCase: SetAppLockTimeoutUseCase by lazy {
        SetAppLockTimeoutUseCase(securityPreferencesRepository)
    }

    override val checkBiometricCapabilityUseCase: CheckBiometricCapabilityUseCase by lazy {
        CheckBiometricCapabilityUseCase(biometricAuthManager)
    }

    override val manageAppLockUseCase: ManageAppLockUseCase by lazy {
        ManageAppLockUseCase(appLockManager)
    }

    override val privateModeManager: PrivateModeManager by lazy {
        PrivateModeManagerImpl()
    }

    override val privatePreferencesRepository: PrivatePreferencesRepository by lazy {
        PrivatePreferencesRepositoryImpl(context)
    }
}
