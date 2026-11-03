package com.pixelmarks.core.update.model

/**
 * Release distribution channel:
 * - STABLE: Official tested releases (no prerelease tags).
 * - BETA: Preview releases (beta, alpha, rc, or marked prerelease on GitHub).
 * - NIGHTLY: Bleeding-edge / continuous automated builds.
 */
enum class ReleaseChannel(val label: String) {
    STABLE("Stable"),
    BETA("Beta"),
    NIGHTLY("Nightly");

    companion object {
        /**
         * Infer channel from GitHub tag name and prerelease flag.
         */
        fun fromTag(tag: String, isPreRelease: Boolean = false): ReleaseChannel {
            val lower = tag.lowercase()
            return when {
                lower.contains("nightly") -> NIGHTLY
                lower.contains("beta") || lower.contains("alpha") || lower.contains("rc") || isPreRelease -> BETA
                else -> STABLE
            }
        }
    }
}
