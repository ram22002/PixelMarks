package com.pixelmarks.domain.usecase

import com.pixelmarks.core.backup.model.BackupExportResult
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.BackupPayload
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.backup.model.RestoreResult
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.data.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

class CreateBackupUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(password: CharArray, outputStream: OutputStream): BackupExportResult {
        return repository.exportBackup(password, outputStream)
    }
}

class InspectBackupUseCase(
    private val repository: BackupRepository
) {
    suspend fun readHeader(inputStream: InputStream): BackupHeaderInfo {
        return repository.readBackupHeader(inputStream)
    }

    suspend fun decryptAndValidate(password: CharArray, inputStream: InputStream): BackupPayload {
        return repository.decryptAndValidate(password, inputStream)
    }
}

class RestoreBackupUseCase(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(payload: BackupPayload, mode: RestoreMode): RestoreResult {
        return repository.restoreBackup(payload, mode)
    }
}

class ObserveLatestBackupUseCase(
    private val repository: BackupRepository
) {
    operator fun invoke(): Flow<List<BackupRecordEntity>> {
        return repository.observeLatestBackupRecord()
    }
}
