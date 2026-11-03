package com.pixelmarks.core.update.model

/**
 * Release distribution channel.
 */
enum class ReleaseChannel(val label: String) {
    STABLE("Stable"),
    BETA("Beta"),
    NIGHTLY("Nightly");

    companion object {
        /**
         * Infer channel from a GitHub release tag, e.g. "v1.2.0-beta.1" → BETA.
         */
        fun fromTag(tag: String): ReleaseChannel {
            val lower = tag.lowercase()
            return when {
                lower.contains("nightly") -> NIGHTLY
                lower.contains("beta") || lower.contains("alpha") || lower.contains("rc") -> BETA
                else -> STABLE
            }
        }
    }
}

