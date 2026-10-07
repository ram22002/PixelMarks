package com.pixelmarks.core.model

/**
 * Domain model representing a bookmark tag.
 */
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: String? = null,
    val isPrivate: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
