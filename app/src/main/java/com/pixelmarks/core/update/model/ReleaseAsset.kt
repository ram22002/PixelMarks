package com.pixelmarks.core.update.model

/**
 * A single APK asset attached to a GitHub release.
 */
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String
)

