package com.pixelmarks.feature.importexport

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.model.html.HtmlExportScope
import com.pixelmarks.core.model.html.HtmlImportCollectionTarget
import com.pixelmarks.core.model.html.HtmlImportDuplicatePolicy
import com.pixelmarks.core.security.PrivateModeManagerImpl
import com.pixelmarks.data.repository.CollectionRepositoryImpl
import com.pixelmarks.data.repository.HtmlBookmarkRepositoryImpl
import com.pixelmarks.domain.usecase.ExportBookmarksHtmlUseCase
import com.pixelmarks.domain.usecase.GetHtmlImportPreviewUseCase
import com.pixelmarks.domain.usecase.ImportBookmarksHtmlUseCase
import com.pixelmarks.domain.usecase.ParseBookmarksHtmlUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HtmlImportExportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var database: PixelMarksDatabase
    private lateinit var privateModeManager: PrivateModeManagerImpl
    private lateinit var viewModel: HtmlImportExportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            context,
            PixelMarksDatabase::class.java
        ).allowMainThreadQueries().build()

        val repository = HtmlBookmarkRepositoryImpl(database, ioDispatcher = Dispatchers.IO)
        val collectionRepo = CollectionRepositoryImpl(database.collectionDao())
        privateModeManager = PrivateModeManagerImpl()

        viewModel = HtmlImportExportViewModel(
            parseBookmarksHtmlUseCase = ParseBookmarksHtmlUseCase(repository),
            getHtmlImportPreviewUseCase = GetHtmlImportPreviewUseCase(repository),
            importBookmarksHtmlUseCase = ImportBookmarksHtmlUseCase(repository),
            exportBookmarksHtmlUseCase = ExportBookmarksHtmlUseCase(repository),
            collectionRepository = collectionRepo,
            privateModeManager = privateModeManager,
            ioDispatcher = Dispatchers.IO
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun waitFor(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(20)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Condition timed out after $timeoutMs ms", condition())
    }

    @Test
    fun testInitialState_isPublicModeByDefault() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isPrivate)
        assertEquals(HtmlExportScope.All, state.exportScope)
        assertNull(state.parseResult)
        assertNull(state.importPreview)
        assertNull(state.importSummary)
    }

    @Test
    fun testPrivacyModeChange_updatesStateAndResetsTransientPreviews() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isPrivate)

        privateModeManager.enterPrivateMode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isPrivate)

        privateModeManager.exitPrivateMode()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPrivate)
    }

    @Test
    fun testUpdateOptions_updatesDuplicatePolicyAndCollectionTarget() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateDuplicatePolicy(HtmlImportDuplicatePolicy.UPDATE)
        assertEquals(HtmlImportDuplicatePolicy.UPDATE, viewModel.uiState.value.importOptions.duplicatePolicy)

        viewModel.updateCollectionTarget(HtmlImportCollectionTarget.RootOnly)
        assertEquals(HtmlImportCollectionTarget.RootOnly, viewModel.uiState.value.importOptions.collectionTarget)

        viewModel.setExportScope(HtmlExportScope.CollectionSubtree(collectionId = 42L, collectionName = "Tech"))
        assertTrue(viewModel.uiState.value.exportScope is HtmlExportScope.CollectionSubtree)
    }

    @Test
    fun testOnFileSelected_parsesHtmlAndSetsPreview() {
        val htmlContent = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
            <TITLE>Bookmarks</TITLE>
            <H1>Bookmarks</H1>
            <DL><p>
                <DT><A HREF="https://example.com/one">Example One</A>
                <DT><A HREF="https://example.com/two">Example Two</A>
            </DL><p>
        """.trimIndent()

        val tempFile = File.createTempFile("bookmarks_test", ".html", context.cacheDir)
        tempFile.writeText(htmlContent)

        viewModel.onFileSelected(Uri.fromFile(tempFile), context.contentResolver)
        waitFor { !viewModel.uiState.value.isParsing }

        val state = viewModel.uiState.value
        assertFalse(state.isParsing)
        assertNotNull(state.parseResult)
        assertEquals(2, state.parseResult?.validBookmarks?.size)
        assertNotNull(state.importPreview)
        assertEquals(2, state.importPreview?.totalFound)
        assertEquals(2, state.importPreview?.validCount)
        assertEquals(0, state.importPreview?.existingDbDuplicateCount)

        tempFile.delete()
    }

    @Test
    fun testConfirmImport_executesImportAndGeneratesSummary() {
        val htmlContent = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
            <TITLE>Bookmarks</TITLE>
            <DL><p>
                <DT><A HREF="https://example.com/one">Example One</A>
                <DT><A HREF="https://example.com/two">Example Two</A>
            </DL><p>
        """.trimIndent()

        val tempFile = File.createTempFile("bookmarks_test", ".html", context.cacheDir)
        tempFile.writeText(htmlContent)

        viewModel.onFileSelected(Uri.fromFile(tempFile), context.contentResolver)
        waitFor { viewModel.uiState.value.parseResult != null }

        viewModel.confirmImport()
        waitFor { viewModel.uiState.value.importSummary != null }

        val state = viewModel.uiState.value
        assertFalse(state.isImporting)
        assertNull(state.parseResult)
        assertNull(state.importPreview)
        assertNotNull(state.importSummary)
        assertEquals(2, state.importSummary?.importedCount)
        assertEquals(0, state.importSummary?.skippedDuplicatesCount)

        // Verify inserted into database
        waitFor { runBlocking { database.bookmarkDao().getAllByPrivacy(isPrivate = false) }.size == 2 }
        val allBookmarks = runBlocking { database.bookmarkDao().getAllByPrivacy(isPrivate = false) }
        assertEquals(2, allBookmarks.size)

        // Dismiss summary
        viewModel.dismissSummary()
        assertNull(viewModel.uiState.value.importSummary)

        tempFile.delete()
    }

    @Test
    fun testCancelImport_clearsImportState() {
        val htmlContent = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <TITLE>Bookmarks</TITLE>
            <DL><p>
                <DT><A HREF="https://example.com/one">Example One</A>
            </DL><p>
        """.trimIndent()

        val tempFile = File.createTempFile("bookmarks_cancel_test", ".html", context.cacheDir)
        tempFile.writeText(htmlContent)

        viewModel.onFileSelected(Uri.fromFile(tempFile), context.contentResolver)
        waitFor { !viewModel.uiState.value.isParsing }

        assertNotNull(viewModel.uiState.value.parseResult)

        viewModel.cancelImport()
        val state = viewModel.uiState.value
        assertNull(state.parseResult)
        assertNull(state.importPreview)
        assertFalse(state.isImporting)

        // Nothing inserted in database
        val allBookmarks = runBlocking { database.bookmarkDao().getAllByPrivacy(isPrivate = false) }
        assertTrue(allBookmarks.isEmpty())

        tempFile.delete()
    }

    @Test
    fun testExportToUri_exportsBookmarksSuccessfully() {
        val now = System.currentTimeMillis()
        // Insert a bookmark to export
        runBlocking {
            database.bookmarkDao().insert(
                BookmarkEntity(
                    id = 1L,
                    url = "https://export-test.com",
                    normalizedUrl = "https://export-test.com",
                    title = "Export Test Title",
                    description = "Export description",
                    domain = "export-test.com",
                    createdAt = now,
                    updatedAt = now,
                    isPrivate = false
                )
            )
        }

        val tempExportFile = File.createTempFile("export_result", ".html", context.cacheDir)

        viewModel.exportToUri(Uri.fromFile(tempExportFile), context.contentResolver)
        waitFor { !viewModel.uiState.value.isExporting }

        val state = viewModel.uiState.value
        assertFalse(state.isExporting)
        assertNotNull(state.exportSuccessMessage)
        assertNull(state.exportErrorMessage)

        val exportedContent = tempExportFile.readText()
        assertTrue(exportedContent.contains("https://export-test.com"))
        assertTrue(exportedContent.contains("Export Test Title"))

        tempExportFile.delete()
    }
}
