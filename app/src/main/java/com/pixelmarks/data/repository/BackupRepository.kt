package com.pixelmarks.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.pixelmarks.core.backup.crypto.BackupCrypto
import com.pixelmarks.core.backup.format.BackupContainer
import com.pixelmarks.core.backup.model.BackupBookmarkDto
import com.pixelmarks.core.backup.model.BackupBookmarkTagRefDto
import com.pixelmarks.core.backup.model.BackupCollectionDto
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupExportResult
import com.pixelmarks.core.backup.model.BackupHeaderInfo
import com.pixelmarks.core.backup.model.BackupPayload
import com.pixelmarks.core.backup.model.BackupPreferencesDto
import com.pixelmarks.core.backup.model.BackupReminderDto
import com.pixelmarks.core.backup.model.BackupTagDto
import com.pixelmarks.core.backup.model.RestoreMode
import com.pixelmarks.core.backup.model.RestoreResult
import com.pixelmarks.core.backup.validation.BackupValidator
import com.pixelmarks.core.browser.BrowserPreference
import com.pixelmarks.core.database.PixelMarksDatabase
import com.pixelmarks.core.database.entity.BackupRecordEntity
import com.pixelmarks.core.database.entity.BookmarkEntity
import com.pixelmarks.core.database.entity.BookmarkTagCrossRef
import com.pixelmarks.core.database.entity.CollectionEntity
import com.pixelmarks.core.database.entity.ReminderEntity
import com.pixelmarks.core.database.entity.TagEntity
import com.pixelmarks.core.database.model.BackupStatus
import com.pixelmarks.core.database.model.BackupType
import com.pixelmarks.core.database.model.MetadataStatus
import com.pixelmarks.core.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

interface BackupRepository {
    suspend fun createBackupPayload(): BackupPayload
    suspend fun exportBackup(password: CharArray, outputStream: OutputStream): BackupExportResult
    suspend fun readBackupHeader(inputStream: InputStream): BackupHeaderInfo
    suspend fun decryptAndValidate(password: CharArray, inputStream: InputStream): BackupPayload
    suspend fun restoreBackup(payload: BackupPayload, mode: RestoreMode): RestoreResult
    fun observeLatestBackupRecord(): Flow<List<BackupRecordEntity>>
    suspend fun getLatestBackupRecord(): BackupRecordEntity?
}

class BackupRepositoryImpl(
    private val context: Context,
    private val database: PixelMarksDatabase,
    private val browserPreferenceRepository: BrowserPreferenceRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupRepository {

    private val gson: Gson = GsonBuilder().create()

    override suspend fun createBackupPayload(): BackupPayload = withContext(ioDispatcher) {
        val bookmarks = database.bookmarkDao().getAll().map { it.toDto() }
        val collections = database.collectionDao().getAll().map { it.toDto() }
        val tags = database.tagDao().getAll().map { it.toDto() }
        val crossRefs = database.bookmarkTagDao().getAllCrossRefs().map {
            BackupBookmarkTagRefDto(bookmarkId = it.bookmarkId, tagId = it.tagId)
        }
        val reminders = database.reminderDao().getAll().map { it.toDto() }
        val pref = browserPreferenceRepository.getPreference().name

        BackupPayload(
            bookmarks = bookmarks,
            collections = collections,
            tags = tags,
            bookmarkTags = crossRefs,
            reminders = reminders,
            preferences = BackupPreferencesDto(browserPreference = pref)
        )
    }

    override suspend fun exportBackup(
        password: CharArray,
        outputStream: OutputStream
    ): BackupExportResult = withContext(ioDispatcher) {
        val payload = createBackupPayload()
        BackupValidator.validate(payload)

        val jsonString = gson.toJson(payload)
        val plaintextBytes = jsonString.toByteArray(Charsets.UTF_8)

        val salt = BackupCrypto.generateSalt()
        val nonce = BackupCrypto.generateNonce()
        val key = BackupCrypto.deriveKey(password, salt)

        val now = System.currentTimeMillis()
        val header = BackupHeaderInfo(
            formatVersion = payload.version,
            createdAt = now,
            kdfIterations = BackupCrypto.KDF_ITERATIONS,
            salt = salt,
            nonce = nonce
        )

        val aad = BackupContainer.createAad(header)
        val ciphertext = BackupCrypto.encrypt(plaintextBytes, key, nonce, aad)

        // Write container envelope
        val digest = MessageDigest.getInstance("SHA-256")
        val hashingStream = object : OutputStream() {
            override fun write(b: Int) {
                digest.update(b.toByte())
                outputStream.write(b)
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                digest.update(b, off, len)
                outputStream.write(b, off, len)
            }
            override fun flush() = outputStream.flush()
        }

        BackupContainer.writeEnvelope(hashingStream, header, ciphertext)
        hashingStream.flush()

        val checksumBytes = digest.digest()
        val checksumHex = checksumBytes.joinToString("") { "%02x".format(it) }

        val totalSize = (4 + 4 + 8 + 4 + 4 + salt.size + 4 + nonce.size + 4 + ciphertext.size).toLong()

        // Record in database
        val record = BackupRecordEntity(
            type = BackupType.LOCAL,
            location = "local_storage",
            createdAt = now,
            sizeBytes = totalSize,
            checksum = checksumHex,
            status = BackupStatus.SUCCESS
        )
        database.backupRecordDao().insert(record)

        BackupExportResult(
            fileSizeBytes = totalSize,
            createdAt = now,
            checksum = checksumHex,
            bookmarkCount = payload.bookmarks.size,
            collectionCount = payload.collections.size,
            tagCount = payload.tags.size
        )
    }

    override suspend fun readBackupHeader(inputStream: InputStream): BackupHeaderInfo = withContext(ioDispatcher) {
        BackupContainer.readHeader(inputStream)
    }

    override suspend fun decryptAndValidate(
        password: CharArray,
        inputStream: InputStream
    ): BackupPayload = withContext(ioDispatcher) {
        val (header, ciphertext) = BackupContainer.readEnvelope(inputStream)
        val key = BackupCrypto.deriveKey(password, header.salt, header.kdfIterations)
        val aad = BackupContainer.createAad(header)

        val decryptedBytes = BackupCrypto.decrypt(ciphertext, key, header.nonce, aad)
        val jsonString = String(decryptedBytes, Charsets.UTF_8)

        val payload = try {
            gson.fromJson(jsonString, BackupPayload::class.java)
        } catch (e: Exception) {
            throw BackupException.CorruptedFile("Failed to parse backup JSON payload: ${e.message}")
        }

        BackupValidator.validate(payload)
        payload
    }

    override suspend fun restoreBackup(
        payload: BackupPayload,
        mode: RestoreMode
    ): RestoreResult = withContext(ioDispatcher) {
        BackupValidator.validate(payload)

        when (mode) {
            RestoreMode.REPLACE -> restoreReplace(payload)
            RestoreMode.MERGE -> restoreMerge(payload, importAllAsNew = false)
            RestoreMode.IMPORT_AS_NEW -> restoreMerge(payload, importAllAsNew = true)
        }
    }

    override fun observeLatestBackupRecord(): Flow<List<BackupRecordEntity>> {
        return database.backupRecordDao().observeAll()
    }

    override suspend fun getLatestBackupRecord(): BackupRecordEntity? = withContext(ioDispatcher) {
        database.backupRecordDao().getLatest()
    }

    private suspend fun restoreReplace(payload: BackupPayload): RestoreResult {
        return database.withTransaction {
            // 1. Cancel existing reminders
            val existingReminders = database.reminderDao().getAll()
            existingReminders.forEach { reminder ->
                ReminderScheduler.cancelReminder(context, reminder.id)
            }

            // 2. Clear current database data in reverse dependency order
            database.reminderDao().deleteAll()
            database.bookmarkTagDao().deleteAll()
            database.bookmarkDao().deleteAll()
            database.collectionDao().deleteAll()
            database.tagDao().deleteAll()

            // 3. Restore Collections
            val restoredCollections = payload.collections.map { it.toEntity() }
            database.collectionDao().insertAll(restoredCollections)

            // 4. Restore Tags
            val restoredTags = payload.tags.map { it.toEntity() }
            database.tagDao().insertAll(restoredTags)

            // 5. Restore Bookmarks
            val restoredBookmarks = payload.bookmarks.map { it.toEntity() }
            database.bookmarkDao().insertAll(restoredBookmarks)

            // 6. Restore Bookmark-Tag CrossRefs
            val restoredRefs = payload.bookmarkTags.map {
                BookmarkTagCrossRef(bookmarkId = it.bookmarkId, tagId = it.tagId)
            }
            database.bookmarkTagDao().insertAll(restoredRefs)

            // 7. Restore Reminders
            val restoredReminders = payload.reminders.map { it.toEntity() }
            for (reminder in restoredReminders) {
                database.reminderDao().insert(reminder)
            }

            // 8. Restore DataStore Preferences
            payload.preferences.browserPreference?.let { prefStr ->
                val pref = BrowserPreference.fromString(prefStr)
                browserPreferenceRepository.setPreference(pref)
            }

            // 9. Reschedule valid future reminders
            val now = System.currentTimeMillis()
            restoredReminders.forEach { reminder ->
                if (!reminder.isCompleted && reminder.reminderAt > now) {
                    val bookmark = database.bookmarkDao().getById(reminder.bookmarkId)
                    if (bookmark != null) {
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            reminderId = reminder.id,
                            bookmarkId = reminder.bookmarkId,
                            title = bookmark.title,
                            url = bookmark.url,
                            triggerAtMillis = reminder.reminderAt
                        )
                    }
                }
            }

            RestoreResult(
                bookmarksRestored = restoredBookmarks.size,
                collectionsRestored = restoredCollections.size,
                tagsRestored = restoredTags.size,
                remindersRestored = restoredReminders.size,
                mode = RestoreMode.REPLACE
            )
        }
    }

    private suspend fun restoreMerge(
        payload: BackupPayload,
        importAllAsNew: Boolean
    ): RestoreResult {
        return database.withTransaction {
            val now = System.currentTimeMillis()

            // 1. Collections: map old ID -> new ID
            val collectionIdMap = mutableMapOf<Long, Long>()
            val existingCollections = database.collectionDao().getAll()

            // Sort collections: root collections first, then children
            val sortedBackupCollections = sortCollectionsByHierarchy(payload.collections)

            for (bCol in sortedBackupCollections) {
                val mappedParentId = bCol.parentId?.let { collectionIdMap[it] ?: it }
                val match = if (!importAllAsNew) {
                    existingCollections.firstOrNull {
                        it.name.equals(bCol.name, ignoreCase = true) && it.parentId == mappedParentId
                    }
                } else null

                if (match != null) {
                    collectionIdMap[bCol.id] = match.id
                } else {
                    val newCol = bCol.toEntity().copy(id = 0, parentId = mappedParentId)
                    val insertedId = database.collectionDao().insert(newCol)
                    collectionIdMap[bCol.id] = insertedId
                }
            }

            // 2. Tags: map old ID -> new ID
            val tagIdMap = mutableMapOf<Long, Long>()
            val existingTags = database.tagDao().getAll()

            for (bTag in payload.tags) {
                val match = if (!importAllAsNew) {
                    existingTags.firstOrNull { it.name.equals(bTag.name, ignoreCase = true) }
                } else null

                if (match != null) {
                    tagIdMap[bTag.id] = match.id
                } else {
                    val newTag = bTag.toEntity().copy(id = 0)
                    val insertedId = database.tagDao().insert(newTag)
                    tagIdMap[bTag.id] = insertedId
                }
            }

            // 3. Bookmarks: map old ID -> new ID
            val bookmarkIdMap = mutableMapOf<Long, Long>()
            var bookmarksInsertedCount = 0

            for (bBookmark in payload.bookmarks) {
                val existing = if (!importAllAsNew) {
                    database.bookmarkDao().getByNormalizedUrl(bBookmark.normalizedUrl).firstOrNull()
                } else null

                if (existing != null) {
                    // Match found: skip duplicate creation, link to existing ID
                    bookmarkIdMap[bBookmark.id] = existing.id
                } else {
                    // Insert as new
                    val targetCollectionId = bBookmark.collectionId?.let { collectionIdMap[it] ?: it }
                    val newBookmark = bBookmark.toEntity().copy(
                        id = 0,
                        collectionId = targetCollectionId
                    )
                    val insertedId = database.bookmarkDao().insert(newBookmark)
                    bookmarkIdMap[bBookmark.id] = insertedId
                    bookmarksInsertedCount++
                }
            }

            // 4. Bookmark-Tag CrossRefs
            for (ref in payload.bookmarkTags) {
                val mappedBookmarkId = bookmarkIdMap[ref.bookmarkId]
                val mappedTagId = tagIdMap[ref.tagId]
                if (mappedBookmarkId != null && mappedTagId != null) {
                    database.bookmarkTagDao().insert(
                        BookmarkTagCrossRef(bookmarkId = mappedBookmarkId, tagId = mappedTagId)
                    )
                }
            }

            // 5. Reminders
            var remindersRestoredCount = 0
            for (bReminder in payload.reminders) {
                val mappedBookmarkId = bookmarkIdMap[bReminder.bookmarkId]
                if (mappedBookmarkId != null) {
                    val existingReminder = database.reminderDao().getByBookmarkId(mappedBookmarkId)
                    if (existingReminder == null) {
                        val newReminder = bReminder.toEntity().copy(
                            id = 0,
                            bookmarkId = mappedBookmarkId
                        )
                        val insertedReminderId = database.reminderDao().insert(newReminder)
                        remindersRestoredCount++

                        if (!newReminder.isCompleted && newReminder.reminderAt > now) {
                            val bookmark = database.bookmarkDao().getById(mappedBookmarkId)
                            if (bookmark != null) {
                                ReminderScheduler.scheduleReminder(
                                    context = context,
                                    reminderId = insertedReminderId,
                                    bookmarkId = mappedBookmarkId,
                                    title = bookmark.title,
                                    url = bookmark.url,
                                    triggerAtMillis = newReminder.reminderAt
                                )
                            }
                        }
                    }
                }
            }

            RestoreResult(
                bookmarksRestored = if (importAllAsNew) payload.bookmarks.size else bookmarksInsertedCount,
                collectionsRestored = collectionIdMap.size,
                tagsRestored = tagIdMap.size,
                remindersRestored = remindersRestoredCount,
                mode = if (importAllAsNew) RestoreMode.IMPORT_AS_NEW else RestoreMode.MERGE
            )
        }
    }

    private fun sortCollectionsByHierarchy(collections: List<BackupCollectionDto>): List<BackupCollectionDto> {
        val result = mutableListOf<BackupCollectionDto>()
        val remaining = collections.toMutableList()
        val processedIds = mutableSetOf<Long>()

        // 1. Root collections (parentId == null)
        val roots = remaining.filter { it.parentId == null }
        result.addAll(roots)
        processedIds.addAll(roots.map { it.id })
        remaining.removeAll(roots)

        // 2. Iteratively add collections whose parentId has been processed
        var changed = true
        while (changed && remaining.isNotEmpty()) {
            val ready = remaining.filter { it.parentId != null && processedIds.contains(it.parentId) }
            if (ready.isNotEmpty()) {
                result.addAll(ready)
                processedIds.addAll(ready.map { it.id })
                remaining.removeAll(ready)
                changed = true
            } else {
                changed = false
            }
        }
        // Add any remaining (e.g. orphans or cycles fallback)
        result.addAll(remaining)
        return result
    }

    private fun BookmarkEntity.toDto() = BackupBookmarkDto(
        id = id,
        url = url,
        normalizedUrl = normalizedUrl,
        title = title,
        description = description,
        domain = domain,
        faviconUrl = faviconUrl,
        thumbnailUrl = thumbnailUrl,
        collectionId = collectionId,
        isFavorite = isFavorite,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        trashedAt = trashedAt,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        visitCount = visitCount,
        reminderAt = reminderAt,
        isPrivate = isPrivate,
        metadataStatus = metadataStatus.name,
        metadataUpdatedAt = metadataUpdatedAt
    )

    private fun BackupBookmarkDto.toEntity() = BookmarkEntity(
        id = id,
        url = url,
        normalizedUrl = normalizedUrl,
        title = title,
        description = description,
        domain = domain,
        faviconUrl = faviconUrl,
        thumbnailUrl = thumbnailUrl,
        collectionId = collectionId,
        isFavorite = isFavorite,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        trashedAt = trashedAt,
        isPrivate = isPrivate,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        visitCount = visitCount,
        reminderAt = reminderAt,
        metadataStatus = try { MetadataStatus.valueOf(metadataStatus) } catch (_: Exception) { MetadataStatus.SUCCESS },
        metadataUpdatedAt = metadataUpdatedAt
    )

    private fun CollectionEntity.toDto() = BackupCollectionDto(
        id = id,
        name = name,
        parentId = parentId,
        icon = icon,
        color = color,
        position = position,
        isLocked = isLocked,
        isHidden = isHidden,
        isPrivate = isPrivate,
        isDomain = isDomain,
        domain = domain,
        description = description,
        keywords = keywords,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun BackupCollectionDto.toEntity() = CollectionEntity(
        id = id,
        name = name,
        parentId = parentId,
        icon = icon,
        color = color,
        position = position,
        isLocked = isLocked,
        isHidden = isHidden,
        isPrivate = isPrivate,
        isDomain = isDomain,
        domain = domain,
        description = description,
        keywords = keywords,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TagEntity.toDto() = BackupTagDto(
        id = id,
        name = name,
        color = color,
        isPrivate = isPrivate,
        createdAt = createdAt
    )

    private fun BackupTagDto.toEntity() = TagEntity(
        id = id,
        name = name,
        color = color,
        isPrivate = isPrivate,
        createdAt = createdAt
    )

    private fun ReminderEntity.toDto() = BackupReminderDto(
        id = id,
        bookmarkId = bookmarkId,
        reminderAt = reminderAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isCompleted = isCompleted
    )

    private fun BackupReminderDto.toEntity() = ReminderEntity(
        id = id,
        bookmarkId = bookmarkId,
        reminderAt = reminderAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isCompleted = isCompleted
    )
}
