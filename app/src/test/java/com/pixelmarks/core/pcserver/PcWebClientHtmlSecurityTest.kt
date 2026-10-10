package com.pixelmarks.core.pcserver

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class PcWebClientHtmlSecurityTest {

    private lateinit var fakeBookmarkRepo: FakeBookmarkRepository2
    private lateinit var fakeCollectionRepo: FakeCollectionRepository2
    private lateinit var fakeDeviceRepo: FakeConnectedDeviceRepository3
    private lateinit var pairingManager: PairingManager
    private lateinit var sessionManager: DeviceSessionManager
    private lateinit var serverManager: LocalServerManager

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private var testPort: Int = 0

    @Before
    fun setup() {
        val tempSocket = ServerSocket(0)
        testPort = tempSocket.localPort
        tempSocket.close()

        fakeBookmarkRepo = FakeBookmarkRepository2()
        fakeCollectionRepo = FakeCollectionRepository2()
        fakeDeviceRepo = FakeConnectedDeviceRepository3()

        pairingManager = PairingManager()
        sessionManager = DeviceSessionManager(fakeDeviceRepo)
        serverManager = LocalServerManager(
            bookmarkRepository = fakeBookmarkRepo,
            collectionRepository = fakeCollectionRepo,
            pairingManager = pairingManager,
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        serverManager.stopServer()
    }

    @Test
    fun testHtmlResponse_hasStrictSecurityHeaders() {
        serverManager.startServer(testPort)

        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/")
            .build()
        val response = httpClient.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("text/html; charset=utf-8", response.header("Content-Type"))
        assertEquals("nosniff", response.header("X-Content-Type-Options"))
        assertEquals("DENY", response.header("X-Frame-Options"))
        assertEquals("no-store", response.header("Cache-Control"))

        val csp = response.header("Content-Security-Policy")
        assertNotNull(csp)
        assertTrue(csp!!.contains("default-src 'self' 'unsafe-inline'"))
        assertTrue(csp.contains("frame-ancestors 'none'"))
        // Confirms remote CDNs and remote scripts are forbidden by CSP
        assertFalse(csp.contains("https://cdn"))
    }

    @Test
    fun testPcClientHtml_isSelfContainedWithoutExternalDependencies() {
        // Must not contain external remote scripts, remote fonts, or trackers
        assertFalse(PC_CLIENT_HTML.contains("https://cdn."))
        assertFalse(PC_CLIENT_HTML.contains("fonts.googleapis.com"))
        assertFalse(PC_CLIENT_HTML.contains("fonts.gstatic.com"))
        assertFalse(PC_CLIENT_HTML.contains("analytics"))
        assertFalse(PC_CLIENT_HTML.contains("telemetry"))
        assertFalse(PC_CLIENT_HTML.contains("tracker"))

        // Uses system serif/sans fonts for the warm editorial theme
        assertTrue(PC_CLIENT_HTML.contains("Georgia"))
        assertTrue(PC_CLIENT_HTML.contains("-apple-system"))
    }

    @Test
    fun testPcClientHtml_hasResponsiveDesignAndViewport() {
        // Contains viewport meta tag
        assertTrue(PC_CLIENT_HTML.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"))

        // Contains responsive breakpoints
        assertTrue(PC_CLIENT_HTML.contains("@media (max-width: 960px)"))
        assertTrue(PC_CLIENT_HTML.contains("@media (max-width: 640px)"))
    }

    @Test
    fun testPcClientHtml_hasStrictUrlSanitizationAndHtmlEscaping() {
        // Verifies sanitizeUrl implementation exists and checks protocol schemes
        assertTrue(PC_CLIENT_HTML.contains("function sanitizeUrl(raw)"))
        assertTrue(PC_CLIENT_HTML.contains("/^https?:\\/\\//i"))
        assertTrue(PC_CLIENT_HTML.contains("protocol === 'http:' || parsed.protocol === 'https:'"))

        // Verifies escapeHtml implementation exists and escapes special chars
        assertTrue(PC_CLIENT_HTML.contains("function escapeHtml(s)"))
        assertTrue(PC_CLIENT_HTML.contains(".replace(/&/g, '&amp;')"))
        assertTrue(PC_CLIENT_HTML.contains(".replace(/</g, '&lt;')"))
        assertTrue(PC_CLIENT_HTML.contains(".replace(/>/g, '&gt;')"))
    }

    @Test
    fun testPcClientHtml_usesSessionScopedStorageForToken() {
        // Prefers session-scoped storage per security requirements
        assertTrue(PC_CLIENT_HTML.contains("sessionStorage.getItem('pixelmarks_token')"))
        assertTrue(PC_CLIENT_HTML.contains("sessionStorage.setItem('pixelmarks_token'"))
        assertTrue(PC_CLIENT_HTML.contains("sessionStorage.removeItem('pixelmarks_token')"))

        // Must NOT use persistent localStorage for the session token
        assertFalse(PC_CLIENT_HTML.contains("localStorage.setItem('pixelmarks_token'"))
    }

    @Test
    fun testPcClientHtml_includesViewSwitcherAndDossierHeader() {
        // View toggle buttons
        assertTrue(PC_CLIENT_HTML.contains("viewGridBtn"))
        assertTrue(PC_CLIENT_HTML.contains("viewListBtn"))
        assertTrue(PC_CLIENT_HTML.contains("view-grid"))
        assertTrue(PC_CLIENT_HTML.contains("view-list"))

        // Collection dossier header & sub-collections
        assertTrue(PC_CLIENT_HTML.contains("collectionDossier"))
        assertTrue(PC_CLIENT_HTML.contains("dossierSubcollections"))

        // Confirmation modals
        assertTrue(PC_CLIENT_HTML.contains("disconnectModal"))
        assertTrue(PC_CLIENT_HTML.contains("detailModal"))
    }

    @Test
    fun testPcClientHtml_includesAccessibilityAndLocalNetworkNotice() {
        // ARIA attributes
        assertTrue(PC_CLIENT_HTML.contains("role=\"banner\""))
        assertTrue(PC_CLIENT_HTML.contains("role=\"navigation\""))
        assertTrue(PC_CLIENT_HTML.contains("role=\"dialog\""))
        assertTrue(PC_CLIENT_HTML.contains("aria-modal=\"true\""))
        assertTrue(PC_CLIENT_HTML.contains("aria-label="))

        // Explains that devices must be on the same Wi-Fi
        assertTrue(PC_CLIENT_HTML.contains("same Wi-Fi or local network"))
    }
}

private class FakeBookmarkRepository2 : com.pixelmarks.data.repository.BookmarkRepository {
    override suspend fun getAllByPrivacy(isPrivate: Boolean) = emptyList<com.pixelmarks.core.model.Bookmark>()
    override suspend fun getById(id: Long) = null
    override fun observeAll(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeById(id: Long) = kotlinx.coroutines.flow.flowOf(null)
    override fun observeByCollection(collectionId: Long, isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeUnsorted(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeFavorites(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observePinned(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeArchived(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeTrashed(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeByDomain(domain: String, isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override fun observeTotalCount(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(0)
    override fun observeTrashedCount(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(0)
    override fun observeBookmarks(query: String, filters: com.pixelmarks.core.model.BookmarkFilters, sort: com.pixelmarks.core.model.BookmarkSortOption, isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Bookmark>())
    override suspend fun findByNormalizedUrl(normalizedUrl: String, isPrivate: Boolean) = emptyList<com.pixelmarks.core.model.Bookmark>()
    override suspend fun insert(bookmark: com.pixelmarks.core.model.Bookmark) = 1L
    override suspend fun update(bookmark: com.pixelmarks.core.model.Bookmark) {}
    override suspend fun delete(bookmark: com.pixelmarks.core.model.Bookmark) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {}
    override suspend fun setPinned(id: Long, isPinned: Boolean) {}
    override suspend fun setArchived(id: Long, isArchived: Boolean) {}
    override suspend fun moveToTrash(id: Long) {}
    override suspend fun restoreFromTrash(id: Long) {}
    override suspend fun permanentlyDelete(id: Long) {}
    override suspend fun recordVisit(id: Long, openedAt: Long) {}
}

private class FakeCollectionRepository2 : com.pixelmarks.data.repository.CollectionRepository {
    override suspend fun getAllByPrivacy(isPrivate: Boolean) = emptyList<com.pixelmarks.core.model.Collection>()
    override suspend fun getById(id: Long) = null
    override fun observeRootCollections(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Collection>())
    override fun observeChildren(parentId: Long, isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Collection>())
    override fun observeAll(isPrivate: Boolean) = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.model.Collection>())
    override suspend fun getByDomain(domain: String) = null
    override suspend fun insert(collection: com.pixelmarks.core.model.Collection) = 1L
    override suspend fun update(collection: com.pixelmarks.core.model.Collection) {}
    override suspend fun delete(collection: com.pixelmarks.core.model.Collection) {}
    override suspend fun deleteById(id: Long) {}
}

private class FakeConnectedDeviceRepository3 : com.pixelmarks.data.repository.ConnectedDeviceRepository {
    override fun observeAllDevices() = kotlinx.coroutines.flow.flowOf(emptyList<com.pixelmarks.core.database.entity.ConnectedDeviceEntity>())
    override suspend fun getActiveDevices() = emptyList<com.pixelmarks.core.database.entity.ConnectedDeviceEntity>()
    override suspend fun getDeviceById(id: Long) = null
    override suspend fun getDeviceByTokenHash(tokenHash: String) = null
    override suspend fun registerDevice(deviceName: String, tokenHash: String, deviceType: com.pixelmarks.core.database.model.DeviceType) = 1L
    override suspend fun updateLastSeen(deviceId: Long) {}
    override suspend fun revokeDevice(deviceId: Long) {}
    override suspend fun revokeAllDevices() {}
    override suspend fun deleteDevice(deviceId: Long) {}
}
