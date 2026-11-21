package com.pixelmarks.feature.changelog

/**
 * Extracts the "What's new" snippet shown in the in-app update dialog.
 *
 * The dialog should only preview the latest release's recent commits (like a
 * changelog ticker), while the full catalog of every release stays on the
 * Changelog screen.
 *
 * Finds a "Recent Commits" / "What's New" / "Changes" markdown heading and returns
 * only that section (capped to [maxLines] bullet lines). Falls back to the whole
 * sanitized body when the release has no recognizable section header.
 */
internal fun extractWhatsNew(body: String, maxLines: Int = 10): String {
    val sanitized = sanitizeReleaseBody(body)
    if (sanitized.isBlank()) return ""

    val headingRegex = Regex(
        pattern = """^#{1,6}\s*.*?(recent commits|what'?s new|whats new|changes)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )
    val lines = sanitized.lines()
    val start = lines.indexOfFirst { headingRegex.containsMatchIn(it.trim()) }
    if (start == -1) return sanitized

    val end = lines.subList(start + 1, lines.size)
        .indexOfFirst { it.trim().startsWith("#") }
        .let { if (it == -1) lines.size else it + start + 1 }

    val section = lines.subList(start + 1, end)
        .filter { it.isNotBlank() }
        .take(maxLines)
        .joinToString("\n")
        .trim()

    return section.ifBlank { sanitized }
}
