package com.pixelmarks.core.pcserver

import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.database.model.DeviceType
import com.pixelmarks.core.pcserver.model.PairingDecision
import com.pixelmarks.core.pcserver.model.PairingState
import com.pixelmarks.data.repository.ConnectedDeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
class PairingAndSessionSecurityTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeDeviceRepository: FakeConnectedDeviceRepository
    private lateinit var sessionManager: DeviceSessionManager
    private lateinit var pairingManager: PairingManager

    @Before
    fun setup() {
        fakeDeviceRepository = FakeConnectedDeviceRepository()
        sessionManager = DeviceSessionManager(fakeDeviceRepository)
        pairingManager = PairingManager(
            scope = testScope,
            codeLifetimeMillis = 1000L, // 1 sec for fast expiration testing
            maxFailedAttempts = 5,
            lockoutDurationMillis = 5000L
        )
    }

    @Test
    fun testPairingCodeGeneration_formatAndExpiry() {
        val active = pairingManager.generateNewCode("http://192.168.1.10:8080")
        assertEquals(6, active.code.length)
        assertTrue(active.formattedCode.contains("-"))
        assertEquals(7, active.formattedCode.length)
        assertTrue(active.expiresAt > System.currentTimeMillis())
        assertTrue(active.qrContent.contains("pairCode=${active.code}"))
    }

    @Test
    fun testPairingInitiation_andExplicitApproval() = testScope.runTest {
        val active = pairingManager.generateNewCode("http://192.168.1.10:8080")
        val result = pairingManager.initiatePairing(
            inputCode = active.code,
            deviceName = "Chrome on Windows",
            clientIp = "192.168.1.15",
            userAgent = "Mozilla/5.0"
        )

        assertTrue(result is PairingManager.InitiateResult.AwaitingApproval)
        val requestId = (result as PairingManager.InitiateResult.AwaitingApproval).requestId

        // Verify state is PendingConfirmation
        val pending = pairingManager.pairingState.value as PairingState.PendingConfirmation
        assertEquals("Chrome on Windows", pending.deviceName)
        assertEquals("192.168.1.15", pending.clientIp)

        // Approve pairing
        launch {
            val approved = pairingManager.approvePairing(requestId, "mock-token")
            assertTrue(approved)
        }

        val decision = pairingManager.awaitDecision(requestId, timeoutMillis = 1000L)
        assertTrue(decision is PairingDecision.Approved)
    }

    @Test
    fun testReplayProtection_consumedCodeCannotBeReused() = testScope.runTest {
        val active = pairingManager.generateNewCode("http://192.168.1.10:8080")
        val code = active.code

        val res1 = pairingManager.initiatePairing(code, "Device1", "192.168.1.15", "")
        assertTrue(res1 is PairingManager.InitiateResult.AwaitingApproval)
        val reqId = (res1 as PairingManager.InitiateResult.AwaitingApproval).requestId

        launch {
            pairingManager.approvePairing(reqId, "token-1")
        }
        val decision = pairingManager.awaitDecision(reqId)
        assertTrue(decision is PairingDecision.Approved)

        // Replay attempt with same code
        val res2 = pairingManager.initiatePairing(code, "Device2", "192.168.1.16", "")
        assertTrue(res2 is PairingManager.InitiateResult.Error)
        val err = res2 as PairingManager.InitiateResult.Error
        assertTrue(err.message.contains("expired or is invalid", ignoreCase = true))
    }

    @Test
    fun testExpiredCode_isRejected() {
        // Pairing manager with 1ms code lifetime
        val fastManager = PairingManager(
            scope = testScope,
            codeLifetimeMillis = 1L
        )
        val active = fastManager.generateNewCode("http://192.168.1.10:8080")
        Thread.sleep(5) // Wait for expiration

        val res = fastManager.initiatePairing(active.code, "PC", "192.168.1.15", "")
        assertTrue(res is PairingManager.InitiateResult.Error)
        assertTrue((res as PairingManager.InitiateResult.Error).message.contains("expired", ignoreCase = true))
    }

    @Test
    fun testBruteForceLockout_triggersAfterFiveFailures() {
        val active = pairingManager.generateNewCode("http://192.168.1.10:8080")

        // 4 failed attempts
        for (i in 1..4) {
            val res = pairingManager.initiatePairing("000000", "Attacker", "192.168.1.99", "")
            assertTrue(res is PairingManager.InitiateResult.Error)
            assertFalse((res as PairingManager.InitiateResult.Error).isLocked)
        }

        // 5th failed attempt -> locks out
        val fifthRes = pairingManager.initiatePairing("000000", "Attacker", "192.168.1.99", "")
        assertTrue(fifthRes is PairingManager.InitiateResult.Error)

        // 6th attempt should return locked
        val sixthRes = pairingManager.initiatePairing(active.code, "Attacker", "192.168.1.99", "")
        assertTrue(sixthRes is PairingManager.InitiateResult.Error)
        assertTrue((sixthRes as PairingManager.InitiateResult.Error).isLocked)
    }

    @Test
    fun testDeviceSessionCreation_hashesTokenAndAuthenticates() = testScope.runTest {
        val (deviceId, plaintextToken) = sessionManager.createDeviceSession("Chrome PC")
        assertTrue(plaintextToken.length > 20)

        // Verify plain token is NOT stored in repo
        val saved = fakeDeviceRepository.getDeviceById(deviceId)
        assertNotNull(saved)
        assertFalse(saved!!.tokenHash == plaintextToken)
        assertEquals(sessionManager.hashToken(plaintextToken), saved.tokenHash)

        // Authenticate with valid token
        val authenticated = sessionManager.authenticateToken(plaintextToken)
        assertNotNull(authenticated)
        assertEquals("Chrome PC", authenticated!!.deviceName)

        // Authenticate with wrong token
        val badAuth = sessionManager.authenticateToken("fake-token-xyz")
        assertNull(badAuth)
    }

    @Test
    fun testRevokeDevice_invalidatesSubsequentAuthentication() = testScope.runTest {
        val (deviceId, token) = sessionManager.createDeviceSession("Work Laptop")
        val authBefore = sessionManager.authenticateToken(token)
        assertNotNull(authBefore)

        // Revoke device
        sessionManager.revokeDevice(deviceId)

        // Authenticate should now fail
        val authAfter = sessionManager.authenticateToken(token)
        assertNull(authAfter)
    }

    @Test
    fun testRevokeAllDevices_invalidatesAllSessions() = testScope.runTest {
        val (_, token1) = sessionManager.createDeviceSession("PC 1")
        val (_, token2) = sessionManager.createDeviceSession("PC 2")

        assertNotNull(sessionManager.authenticateToken(token1))
        assertNotNull(sessionManager.authenticateToken(token2))

        sessionManager.revokeAllDevices()

        assertNull(sessionManager.authenticateToken(token1))
        assertNull(sessionManager.authenticateToken(token2))
    }
}

private class FakeConnectedDeviceRepository : ConnectedDeviceRepository {
    private val devices = mutableListOf<ConnectedDeviceEntity>()
    private val idCounter = AtomicLong(1L)
    private val devicesFlow = MutableStateFlow<List<ConnectedDeviceEntity>>(emptyList())

    override fun observeAllDevices(): Flow<List<ConnectedDeviceEntity>> = devicesFlow.asStateFlow()

    override suspend fun getActiveDevices(): List<ConnectedDeviceEntity> {
        return devices.filter { it.revokedAt == null && !it.isBlocked }
    }

    override suspend fun getDeviceById(id: Long): ConnectedDeviceEntity? {
        return devices.find { it.id == id }
    }

    override suspend fun getDeviceByTokenHash(tokenHash: String): ConnectedDeviceEntity? {
        return devices.find { it.tokenHash == tokenHash }
    }

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
        devicesFlow.value = devices.toList()
        return id
    }

    override suspend fun updateLastSeen(deviceId: Long) {
        val index = devices.indexOfFirst { it.id == deviceId }
        if (index >= 0) {
            devices[index] = devices[index].copy(lastSeenAt = System.currentTimeMillis())
            devicesFlow.value = devices.toList()
        }
    }

    override suspend fun revokeDevice(deviceId: Long) {
        val index = devices.indexOfFirst { it.id == deviceId }
        if (index >= 0) {
            devices[index] = devices[index].copy(revokedAt = System.currentTimeMillis())
            devicesFlow.value = devices.toList()
        }
    }

    override suspend fun revokeAllDevices() {
        val now = System.currentTimeMillis()
        for (i in devices.indices) {
            devices[i] = devices[i].copy(revokedAt = now)
        }
        devicesFlow.value = devices.toList()
    }

    override suspend fun deleteDevice(deviceId: Long) {
        devices.removeAll { it.id == deviceId }
        devicesFlow.value = devices.toList()
    }
}
