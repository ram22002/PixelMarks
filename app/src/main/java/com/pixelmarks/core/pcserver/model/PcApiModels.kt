package com.pixelmarks.core.pcserver.model

/**
 * Data models for the PixelMarks PC Connect HTTP REST API.
 */
data class ServerStatusResponse(
    val appName: String = "PixelMarks",
    val version: String = "1.0.0",
    val status: String = "READY",
    val requiresPairing: Boolean = true
)

data class PairRequest(
    val pairingCode: String,
    val deviceName: String = "PC Browser",
    val userAgent: String = ""
)

data class PairResponse(
    val success: Boolean,
    val token: String? = null,
    val requestId: String? = null,
    val status: String,
    val message: String
)

data class PairStatusResponse(
    val status: String, // PENDING, APPROVED, DENIED, EXPIRED, LOCKED
    val token: String? = null,
    val message: String? = null
)

data class BookmarkDto(
    val id: Long,
    val url: String,
    val title: String,
    val description: String?,
    val collectionId: Long?,
    val collectionName: String?,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val visitCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class CollectionDto(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val icon: String?,
    val color: String?,
    val bookmarkCount: Int = 0
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val status: Int
)
