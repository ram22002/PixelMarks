package com.pixelmarks.core.update.model

/**
 * Robust Semantic Version 2.0.0 implementation supporting:
 * - Standard major.minor.patch versions: "1.0.0", "v1.0.1"
 * - Prerelease identifiers: "1.0.0-beta", "1.0.0-beta.1", "1.0.0-rc.2"
 * - Build metadata: "1.0.0+20261103" (ignored in precedence comparison)
 * - Optional leading "v" or "V"
 * - Graceful fallback on malformed or partial version strings
 *
 * Precedence rules adhere to SemVer 2.0.0 specification (item 11):
 * 1. Compare major, minor, and patch numerically.
 * 2. When major, minor, and patch are equal, a normal release has HIGHER precedence than a pre-release:
 *    e.g., 1.0.0 > 1.0.0-beta.
 * 3. Two pre-release versions with the same major.minor.patch:
 *    Compare dot-separated identifiers left to right:
 *    - Numeric identifiers are compared numerically: "1" < "2" < "10".
 *    - Identifiers with letters/hyphens are compared lexically in ASCII sort order: "alpha" < "beta" < "rc".
 *    - Numeric identifiers always have lower precedence than non-numeric identifiers: "1" < "beta".
 *    - A larger set of pre-release fields has higher precedence if preceding ones are equal: "1.0.0-beta" < "1.0.0-beta.1".
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val buildMetadata: String? = null,
    val raw: String = ""
) : Comparable<SemVer> {

    val isPreRelease: Boolean get() = !preRelease.isNullOrBlank()

    /** Clean version display string without leading 'v'. */
    val displayString: String
        get() = buildString {
            append("$major.$minor.$patch")
            if (!preRelease.isNullOrBlank()) {
                append("-$preRelease")
            }
        }

    override fun compareTo(other: SemVer): Int {
        if (this.major != other.major) return this.major.compareTo(other.major)
        if (this.minor != other.minor) return this.minor.compareTo(other.minor)
        if (this.patch != other.patch) return this.patch.compareTo(other.patch)

        // Same major.minor.patch:
        // Version without preRelease > version with preRelease (SemVer rule 11.3)
        return when {
            this.preRelease == null && other.preRelease == null -> 0
            this.preRelease == null && other.preRelease != null -> 1 // 1.0.0 > 1.0.0-beta
            this.preRelease != null && other.preRelease == null -> -1 // 1.0.0-beta < 1.0.0
            else -> comparePreRelease(this.preRelease!!, other.preRelease!!)
        }
    }

    private fun comparePreRelease(pr1: String, pr2: String): Int {
        val parts1 = pr1.split(".")
        val parts2 = pr2.split(".")
        val minSize = minOf(parts1.size, parts2.size)

        for (i in 0 until minSize) {
            val id1 = parts1[i]
            val id2 = parts2[i]
            if (id1 == id2) continue

            val num1 = id1.toLongOrNull()
            val num2 = id2.toLongOrNull()

            return when {
                num1 != null && num2 != null -> num1.compareTo(num2)
                num1 != null && num2 == null -> -1 // Numeric identifier < non-numeric
                num1 == null && num2 != null -> 1  // Non-numeric identifier > numeric
                else -> {
                    // Both non-numeric: check common release tokens (alpha < beta < rc)
                    val rank1 = specialPreReleaseRank(id1)
                    val rank2 = specialPreReleaseRank(id2)
                    if (rank1 != rank2) {
                        rank1.compareTo(rank2)
                    } else {
                        id1.compareTo(id2, ignoreCase = true)
                    }
                }
            }
        }

        // Preceding identifiers are equal: larger set of pre-release fields has higher precedence
        return parts1.size.compareTo(parts2.size)
    }

    private fun specialPreReleaseRank(token: String): Int {
        val lower = token.lowercase()
        return when {
            lower.startsWith("alpha") -> 1
            lower.startsWith("preview") -> 2
            lower.startsWith("beta") -> 3
            lower.startsWith("rc") -> 4
            else -> 10
        }
    }

    companion object {
        val ZERO = SemVer(0, 0, 0, raw = "0.0.0")

        /**
         * Safely parse a version string into a [SemVer] instance. Never throws.
         * Handles "v1.2.3-beta.1+build2026", "1.0", "v2", or malformed strings.
         */
        fun parse(input: String?): SemVer {
            if (input.isNullOrBlank()) return ZERO
            val trimmed = input.trim().trimStart('v', 'V')

            return try {
                // Strip build metadata first (after '+')
                val withoutBuild = trimmed.substringBefore('+')
                val buildMeta = if (trimmed.contains('+')) trimmed.substringAfter('+') else null

                // Strip prerelease (after '-')
                val versionCore = withoutBuild.substringBefore('-')
                val preRelease = if (withoutBuild.contains('-')) withoutBuild.substringAfter('-') else null

                // Parse core numbers (dot-separated)
                val parts = versionCore.split(".")
                val major = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val minor = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val patch = parts.getOrNull(2)?.filter { it.isDigit() }?.toIntOrNull() ?: 0

                SemVer(
                    major = major,
                    minor = minor,
                    patch = patch,
                    preRelease = preRelease?.ifBlank { null },
                    buildMetadata = buildMeta?.ifBlank { null },
                    raw = input
                )
            } catch (_: Exception) {
                ZERO
            }
        }
    }
}

