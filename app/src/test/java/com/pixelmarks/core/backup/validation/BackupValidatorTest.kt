package com.pixelmarks.core.backup.validation

import com.pixelmarks.core.backup.model.BackupBookmarkDto
import com.pixelmarks.core.backup.model.BackupBookmarkTagRefDto
import com.pixelmarks.core.backup.model.BackupCollectionDto
import com.pixelmarks.core.backup.model.BackupException
import com.pixelmarks.core.backup.model.BackupPayload
import com.pixelmarks.core.backup.model.BackupReminderDto
import com.pixelmarks.core.backup.model.BackupTagDto
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupValidatorTest {

    private fun sampleValidPayload(): BackupPayload {
        return BackupPayload(
            version = 1,
            bookmarks = listOf(
                BackupBookmarkDto(
                    id = 1L,
                    url = "https://example.com/page",
                    normalizedUrl = "https://example.com/page",
                    title = "Example Page",
                    domain = "example.com",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            ),
            collections = listOf(
                BackupCollectionDto(
                    id = 10L,
                    name = "Tech",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            ),
            tags = listOf(
                BackupTagDto(id = 20L, name = "Kotlin", createdAt = 1000L)
            ),
            bookmarkTags = listOf(
                BackupBookmarkTagRefDto(bookmarkId = 1L, tagId = 20L)
            ),
            reminders = listOf(
                BackupReminderDto(id = 30L, bookmarkId = 1L, reminderAt = 2000L, createdAt = 1000L, updatedAt = 1000L)
            )
        )
    }

    @Test
    fun `valid payload passes validation without error`() {
        val payload = sampleValidPayload()
        BackupValidator.validate(payload)
    }

    @Test
    fun `blank bookmark url throws ValidationError`() {
        val payload = sampleValidPayload().copy(
            bookmarks = listOf(
                sampleValidPayload().bookmarks[0].copy(url = "   ")
            )
        )
        try {
            BackupValidator.validate(payload)
            fail("Expected ValidationError for blank URL")
        } catch (e: BackupException.ValidationError) {
            assertTrue(e.message!!.contains("blank URL"))
        }
    }

    @Test
    fun `unsupported scheme throws ValidationError`() {
        val payload = sampleValidPayload().copy(
            bookmarks = listOf(
                sampleValidPayload().bookmarks[0].copy(url = "javascript:alert(1)")
            )
        )
        try {
            BackupValidator.validate(payload)
            fail("Expected ValidationError for javascript scheme")
        } catch (e: BackupException.ValidationError) {
            assertTrue(e.message!!.contains("invalid URL"))
        }
    }

    @Test
    fun `collection self-parenting throws ValidationError`() {
        val payload = sampleValidPayload().copy(
            collections = listOf(
                BackupCollectionDto(id = 5L, name = "Loop", parentId = 5L, createdAt = 1000L, updatedAt = 1000L)
            )
        )
        try {
            BackupValidator.validate(payload)
            fail("Expected ValidationError for self-parent")
        } catch (e: BackupException.ValidationError) {
            assertTrue(e.message!!.contains("cannot be its own parent"))
        }
    }

    @Test
    fun `collection cycle throws ValidationError`() {
        val payload = sampleValidPayload().copy(
            collections = listOf(
                BackupCollectionDto(id = 1L, name = "A", parentId = 2L, createdAt = 1000L, updatedAt = 1000L),
                BackupCollectionDto(id = 2L, name = "B", parentId = 1L, createdAt = 1000L, updatedAt = 1000L)
            )
        )
        try {
            BackupValidator.validate(payload)
            fail("Expected ValidationError for cyclic hierarchy")
        } catch (e: BackupException.ValidationError) {
            assertTrue(e.message!!.contains("Cyclic parent reference"))
        }
    }

    @Test
    fun `dangling bookmark tag reference throws ValidationError`() {
        val payload = sampleValidPayload().copy(
            bookmarkTags = listOf(
                BackupBookmarkTagRefDto(bookmarkId = 1L, tagId = 999L) // Non-existent tag 999
            )
        )
        try {
            BackupValidator.validate(payload)
            fail("Expected ValidationError for missing tag ref")
        } catch (e: BackupException.ValidationError) {
            assertTrue(e.message!!.contains("missing tag"))
        }
    }

    @Test
    fun `dangling reminder reference throws ValidationError`() {
        val payload = sampleValidPayload().copy(
            reminders = listOf(
                BackupReminderDto(id = 1L, bookmarkId = 888L, reminderAt = 2000L, createdAt = 1000L, updatedAt = 1000L)
            )
        )
        try {
            BackupValidator.validate(payload)
            fail("Expected ValidationError for missing bookmark in reminder")
        } catch (e: BackupException.ValidationError) {
            assertTrue(e.message!!.contains("missing bookmark"))
        }
    }
}
