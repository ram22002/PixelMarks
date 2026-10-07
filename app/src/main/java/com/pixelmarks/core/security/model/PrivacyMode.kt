package com.pixelmarks.core.security.model

/**
 * Runtime privacy mode of the application.
 * Never persisted to disk. Always defaults to PUBLIC on startup and backgrounding.
 */
enum class PrivacyMode {
    PUBLIC,
    PRIVATE
}
