package com.pixelmarks.core.pcserver

import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.database.model.DeviceType
import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.BookmarkFilters
import com.pixelmarks.core.model.BookmarkSortOption
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.pcserver.model.PcServerState
import com.pixelmarks.data.repository.BookmarkRepository
import com.pixelmarks.data.repository.CollectionRepository
import com.pixelmarks.data.repository.ConnectedDeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class LocalServerManagerTest {

    private lateinit var fakeBookmarkRepo: FakeBookmarkRepository
    private lateinit var fakeCollectionRepo: FakeCollectionRepository
    private lateinit var fakeDeviceRepo: FakeConnectedDeviceRepository2
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
        // Find a random available port
        val tempSocket = ServerSocket(0)
        testPort = tempSocket.localPort
        tempSocket.close()

        fakeBookmarkRepo = FakeBookmarkRepository()
        fakeCollectionRepo = FakeCollectionRepository()
        fakeDeviceRepo = FakeConnectedDeviceRepository2()

        pairingManager = PairingManager()
        sessionManager = DeviceSessionManager(fakeDeviceRepo)
        serverManager = LocalServerManager(
            bookmarkRepository = fakeBookmarkRepo,
            collectionRepository = fakeCollectionRepo,
            pairingManager = pairingManager,
            sessionManager = sessionManager
        )

        // Seed bookmarks (public and private)
        fakeBookmarkRepo.insertDirect(
            Bookmark(
                id = 101,
                url = "https://example.com/public",
                normalizedUrl = "example.com/public",
                domain = "example.com",
                title = "Public Bookmark",
                isPrivate = false,
                isTrashed = false
            )
        )
        fakeBookmarkRepo.insertDirect(
            Bookmark(
                id = 102,
                url = "https://example.com/secret",
                normalizedUrl = "example.com/secret",
                domain = "example.com",
                title = "Top Secret Bookmark",
                isPrivate = true, // Private!
                isTrashed = false
            )
        )
        fakeBookmarkRepo.insertDirect(
            Bookmark(
                id = 103,
                url = "https://example.com/trashed",
                normalizedUrl = "example.com/trashed",
                domain = "example.com",
                title = "Trashed Bookmark",
                isPrivate = false,
                isTrashed = true // Trashed!
            )
        )

        // Seed collections
        fakeCollectionRepo.insertDirect(
            Collection(
                id = 201,
                name = "Public Work",
                isPrivate = false
            )
        )
        fakeCollectionRepo.insertDirect(
            Collection(
                id = 202,
                name = "Private Vault",
                isPrivate = true // Private!
            )
        )
    }

    @After
    fun tearDown() {
        serverManager.stopServer()
    }

    @Test
    fun testServerLifecycle_startAndStop() {
        assertFalse(serverManager.serverState.value is PcServerState.Running)
        val started = serverManager.startServer(testPort)
        assertTrue(started)
        assertTrue(serverManager.serverState.value is PcServerState.Running)

        serverManager.stopServer()
        assertEquals(PcServerState.Stopped, serverManager.serverState.value)
    }

    @Test
    fun testLandingPage_andPublicStatusEndpoint() {
        serverManager.startServer(testPort)

        // Test GET /
        val indexRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/")
            .build()
        val indexResponse = httpClient.newCall(indexRequest).execute()
        assertEquals(200, indexResponse.code)
        val indexBody = indexResponse.body?.string() ?: ""
        assertTrue(indexBody.contains("PixelMarks PC Connect"))

        // Test GET /api/v1/status
        val statusRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/status")
            .build()
        val statusResponse = httpClient.newCall(statusRequest).execute()
        assertEquals(200, statusResponse.code)
        val statusBody = statusResponse.body?.string() ?: ""
        assertTrue(statusBody.contains("PixelMarks"))
        assertTrue(statusBody.contains("READY"))
    }

    @Test
    fun testPairing_invalidCodeReturns400() {
        serverManager.startServer(testPort)

        val pairRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/pair")
            .post("{\"pairingCode\":\"999999\",\"deviceName\":\"Test PC\"}".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(pairRequest).execute()
        assertEquals(400, response.code)
        val body = response.body?.string() ?: ""
        assertTrue(body.contains("INVALID_CODE") || body.contains("expired"))
    }

    @Test
    fun testOversizedPayload_returns413() {
        serverManager.startServer(testPort)

        // Payload > 64 KB (65,536 bytes)
        val bigData = "X".repeat(70_000)
        val bigRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/pair")
            .post(bigData.toRequestBody("text/plain".toMediaType()))
            .build()

        val response = httpClient.newCall(bigRequest).execute()
        assertEquals(413, response.code)
    }

    @Test
    fun testCrossSiteRequest_isRejectedWith403() {
        serverManager.startServer(testPort)

        val crossSiteRequest = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/status")
            .header("Sec-Fetch-Site", "cross-site")
            .build()

        val response = httpClient.newCall(crossSiteRequest).execute()
        assertEquals(403, response.code)
    }

    @Test
    fun testUnauthorizedRequest_returns401() {
        serverManager.startServer(testPort)

        // No token
        val reqNoToken = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/bookmarks")
            .build()
        val resNoToken = httpClient.newCall(reqNoToken).execute()
        assertEquals(401, resNoToken.code)

        // Bad token
        val reqBadToken = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/bookmarks")
            .header("Authorization", "Bearer invalid-token-12345")
            .build()
        val resBadToken = httpClient.newCall(reqBadToken).execute()
        assertEquals(401, resBadToken.code)
    }

    @Test
    fun testSecureFolderIsolation_neverExposesPrivateBookmarksOrCollections() = runBlocking {
        serverManager.startServer(testPort)

        // Generate valid session token
        val (_, token) = sessionManager.createDeviceSession("Authorized PC")

        // 1. Fetch bookmarks listing
        val bookmarksReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/bookmarks")
            .header("Authorization", "Bearer $token")
            .build()
        val bookmarksRes = httpClient.newCall(bookmarksReq).execute()
        assertEquals(200, bookmarksRes.code)
        val bookmarksBody = bookmarksRes.body?.string() ?: ""

        // Public bookmark MUST be present
        assertTrue(bookmarksBody.contains("Public Bookmark"))
        assertTrue(bookmarksBody.contains("https://example.com/public"))

        // Secret / Private bookmark MUST NEVER be present!
        assertFalse(bookmarksBody.contains("Top Secret Bookmark"))
        assertFalse(bookmarksBody.contains("https://example.com/secret"))

        // Trashed bookmark MUST NOT be present
        assertFalse(bookmarksBody.contains("Trashed Bookmark"))

        // 2. Fetch direct detail of private bookmark -> MUST return 404 Not Found!
        val privateDetailReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/bookmarks/102")
            .header("Authorization", "Bearer $token")
            .build()
        val privateDetailRes = httpClient.newCall(privateDetailReq).execute()
        assertEquals(404, privateDetailRes.code)

        // 3. Fetch public bookmark detail -> MUST return 200 OK
        val publicDetailReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/bookmarks/101")
            .header("Authorization", "Bearer $token")
            .build()
        val publicDetailRes = httpClient.newCall(publicDetailReq).execute()
        assertEquals(200, publicDetailRes.code)

        // 4. Fetch collections listing -> private collections MUST NEVER be present!
        val collectionsReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/collections")
            .header("Authorization", "Bearer $token")
            .build()
        val collectionsRes = httpClient.newCall(collectionsReq).execute()
        assertEquals(200, collectionsRes.code)
        val collectionsBody = collectionsRes.body?.string() ?: ""

        assertTrue(collectionsBody.contains("Public Work"))
        assertFalse(collectionsBody.contains("Private Vault"))
    }

    @Test
    fun testRevokeSessionEndpoint() = runBlocking {
        serverManager.startServer(testPort)

        val (_, token) = sessionManager.createDeviceSession("Browser To Revoke")

        // Call POST /api/v1/revoke
        val revokeReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/revoke")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody())
            .build()
        val revokeRes = httpClient.newCall(revokeReq).execute()
        assertEquals(200, revokeRes.code)

        // Subsequent call to /api/v1/bookmarks with that token must be 401 Unauthorized
        val bookmarksReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/api/v1/bookmarks")
            .header("Authorization", "Bearer $token")
            .build()
        val bookmarksRes = httpClient.newCall(bookmarksReq).execute()
        assertEquals(401, bookmarksRes.code)
    }
}

private class FakeBookmarkRepository : BookmarkRepository {
    private val list = mutableListOf<Bookmark>()

    fun insertDirect(b: Bookmark) { list.add(b) }

    override suspend fun getAllByPrivacy(isPrivate: Boolean): List<Bookmark> {
        return list.filter { it.isPrivate == isPrivate }
    }

    override suspend fun getById(id: Long): Bookmark? {
        return list.find { it.id == id }
    }

    override fun observeAll(isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(list.filter { it.isPrivate == isPrivate })
    override fun observeById(id: Long): Flow<Bookmark?> = flowOf(list.find { it.id == id })
    override fun observeByCollection(collectionId: Long, isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observeUnsorted(isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observeFavorites(isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observePinned(isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observeArchived(isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observeTrashed(isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observeByDomain(domain: String, isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override fun observeTotalCount(isPrivate: Boolean): Flow<Int> = flowOf(list.size)
    override fun observeTrashedCount(isPrivate: Boolean): Flow<Int> = flowOf(0)
    override fun observeBookmarks(query: String, filters: BookmarkFilters, sort: BookmarkSortOption, isPrivate: Boolean): Flow<List<Bookmark>> = flowOf(emptyList())
    override suspend fun findByNormalizedUrl(normalizedUrl: String, isPrivate: Boolean): List<Bookmark> = emptyList()
    override suspend fun insert(bookmark: Bookmark): Long = 1L
    override suspend fun update(bookmark: Bookmark) {}
    override suspend fun delete(bookmark: Bookmark) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {}
    override suspend fun setPinned(id: Long, isPinned: Boolean) {}
    override suspend fun setArchived(id: Long, isArchived: Boolean) {}
    override suspend fun moveToTrash(id: Long) {}
    override suspend fun restoreFromTrash(id: Long) {}
    override suspend fun permanentlyDelete(id: Long) {}
    override suspend fun recordVisit(id: Long, openedAt: Long) {}
}

private class FakeCollectionRepository : CollectionRepository {
    private val list = mutableListOf<Collection>()

    fun insertDirect(c: Collection) { list.add(c) }

    override suspend fun getAllByPrivacy(isPrivate: Boolean): List<Collection> {
        return list.filter { it.isPrivate == isPrivate }
    }

    override suspend fun getById(id: Long): Collection? = list.find { it.id == id }
    override fun observeRootCollections(isPrivate: Boolean): Flow<List<Collection>> = flowOf(emptyList())
    override fun observeChildren(parentId: Long, isPrivate: Boolean): Flow<List<Collection>> = flowOf(emptyList())
    override fun observeAll(isPrivate: Boolean): Flow<List<Collection>> = flowOf(list.filter { it.isPrivate == isPrivate })
    override suspend fun getByDomain(domain: String): Collection? = null
    override suspend fun insert(collection: Collection): Long = 1L
    override suspend fun update(collection: Collection) {}
    override suspend fun delete(collection: Collection) {}
    override suspend fun deleteById(id: Long) {}
}

private class FakeConnectedDeviceRepository2 : ConnectedDeviceRepository {
    private val devices = mutableListOf<ConnectedDeviceEntity>()
    private val idCounter = AtomicLong(1L)

    override fun observeAllDevices(): Flow<List<ConnectedDeviceEntity>> = flowOf(devices)
    override suspend fun getActiveDevices(): List<ConnectedDeviceEntity> = devices.filter { it.revokedAt == null && !it.isBlocked }
    override suspend fun getDeviceById(id: Long): ConnectedDeviceEntity? = devices.find { it.id == id }
    override suspend fun getDeviceByTokenHash(tokenHash: String): ConnectedDeviceEntity? = devices.find { it.tokenHash == tokenHash }
    override suspend fun registerDevice(deviceName: String, tokenHash: String, deviceType: DeviceType): Long {
        val id = idCounter.getAndIncrement()
        val now = System.currentTimeMillis()
        val entity = ConnectedDeviceEntity(
            id = id,
            deviceName = deviceName,
            tokenHash = tokenHash,
            deviceType = deviceType,
            createdAt = now,
            lastSeenAt = now
        )
        devices.add(entity)
        return id
    }
    override suspend fun updateLastSeen(deviceId: Long) {}
    override suspend fun revokeDevice(deviceId: Long) {
        val idx = devices.indexOfFirst { it.id == deviceId }
        if (idx >= 0) devices[idx] = devices[idx].copy(revokedAt = System.currentTimeMillis())
    }
    override suspend fun revokeAllDevices() {}
    override suspend fun deleteDevice(deviceId: Long) {}
}
