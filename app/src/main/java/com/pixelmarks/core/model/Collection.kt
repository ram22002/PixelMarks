package com.pixelmarks.core.model

/**
 * Domain model representing a hierarchical bookmark collection/folder.
 */
data class Collection(
    val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val icon: String? = null,
    val color: String? = null,
    val position: Int = 0,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val isDomain: Boolean = false,
    val domain: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
