package com.pixelmarks.data.repository

import com.pixelmarks.core.database.dao.ConnectedDeviceDao
import com.pixelmarks.core.database.entity.ConnectedDeviceEntity
import com.pixelmarks.core.database.model.DeviceType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing paired PC browser devices.
 */
interface ConnectedDeviceRepository {
    fun observeAllDevices(): Flow<List<ConnectedDeviceEntity>>
    suspend fun getActiveDevices(): List<ConnectedDeviceEntity>
    suspend fun getDeviceById(id: Long): ConnectedDeviceEntity?
    suspend fun getDeviceByTokenHash(tokenHash: String): ConnectedDeviceEntity?
    suspend fun registerDevice(deviceName: String, tokenHash: String, deviceType: DeviceType = DeviceType.BROWSER): Long
    suspend fun updateLastSeen(deviceId: Long)
    suspend fun revokeDevice(deviceId: Long)
    suspend fun revokeAllDevices()
    suspend fun deleteDevice(deviceId: Long)
}

class ConnectedDeviceRepositoryImpl(
    private val dao: ConnectedDeviceDao
) : ConnectedDeviceRepository {

    override fun observeAllDevices(): Flow<List<ConnectedDeviceEntity>> {
        return dao.observeAll()
    }

    override suspend fun getActiveDevices(): List<ConnectedDeviceEntity> {
        return dao.getActiveDevices()
    }

    override suspend fun getDeviceById(id: Long): ConnectedDeviceEntity? {
        return dao.getById(id)
    }

    override suspend fun getDeviceByTokenHash(tokenHash: String): ConnectedDeviceEntity? {
        return dao.getByTokenHash(tokenHash)
    }

    override suspend fun registerDevice(
        deviceName: String,
        tokenHash: String,
        deviceType: DeviceType
    ): Long {
        val now = System.currentTimeMillis()
        val entity = ConnectedDeviceEntity(
            deviceName = deviceName,
            deviceType = deviceType,
            tokenHash = tokenHash,
            createdAt = now,
            lastSeenAt = now
        )
        return dao.insert(entity)
    }

    override suspend fun updateLastSeen(deviceId: Long) {
        val device = dao.getById(deviceId) ?: return
        dao.update(device.copy(lastSeenAt = System.currentTimeMillis()))
    }

    override suspend fun revokeDevice(deviceId: Long) {
        val device = dao.getById(deviceId) ?: return
        dao.update(device.copy(revokedAt = System.currentTimeMillis()))
    }

    override suspend fun revokeAllDevices() {
        val now = System.currentTimeMillis()
        val devices = dao.getActiveDevices()
        devices.forEach { device ->
            dao.update(device.copy(revokedAt = now))
        }
    }

    override suspend fun deleteDevice(deviceId: Long) {
        dao.deleteById(deviceId)
    }
}
