package com.pixelmarks.core.html

import com.pixelmarks.core.model.html.HtmlBookmarkItem
import com.pixelmarks.core.model.html.HtmlInvalidEntry
import com.pixelmarks.core.model.html.HtmlParseResult
import com.pixelmarks.core.util.UrlUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

/**
 * Standards-compliant, secure Netscape Bookmark HTML parser.
 * Safely parses bookmarks, folder trees, timestamps, tags, and descriptions without script execution.
 */
object NetscapeBookmarkParser {

    /**
     * Maximum allowed HTML bookmark file size (15 MB) to guard against memory exhaustion.
     */
    const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024L

    /**
     * Parses an input stream of Netscape Bookmark HTML.
     * Enforces size limits, rejects malicious protocols, and validates all URLs.
     */
    fun parse(inputStream: InputStream): HtmlParseResult {
        // Read bounded bytes to protect against unbounded memory attacks
        val buffer = ByteArray(8192)
        val baos = ByteArrayOutputStream()
        var totalBytesRead = 0L
        var read: Int

        while (inputStream.read(buffer).also { read = it } != -1) {
            totalBytesRead += read
            if (totalBytesRead > MAX_FILE_SIZE_BYTES) {
                throw IllegalArgumentException("Bookmark file exceeds maximum size limit of 15 MB")
            }
            baos.write(buffer, 0, read)
        }

        if (totalBytesRead == 0L) {
            return HtmlParseResult(
                totalFound = 0,
                validBookmarks = emptyList(),
                invalidEntries = emptyList(),
                detectedFolders = emptyList(),
                fileDuplicateCount = 0
            )
        }

        val htmlBytes = baos.toByteArray()
        val document: Document = Jsoup.parse(htmlBytes.inputStream(), "UTF-8", "")

        val validBookmarks = mutableListOf<HtmlBookmarkItem>()
        val invalidEntries = mutableListOf<HtmlInvalidEntry>()
        val detectedFolders = linkedSetOf<String>()
        val seenNormalizedUrls = mutableSetOf<String>()
        var fileDuplicatesCount = 0

        // Find the root <DL> or fall back to <body>
        val rootDl = document.selectFirst("dl") ?: document.body()

        fun processBookmarkAnchor(
            anchor: Element,
            currentPath: List<String>,
            description: String?
        ) {
            val rawUrl = anchor.attr("href").trim()
            val rawTitle = anchor.text().trim()

            if (rawUrl.isEmpty()) {
                invalidEntries.add(
                    HtmlInvalidEntry(
                        rawUrl = null,
                        title = rawTitle.takeIf { it.isNotEmpty() },
                        reason = "Missing or empty URL"
                    )
                )
                return
            }

            // Strictly check for HTTP / HTTPS protocol
            val lowerUrl = rawUrl.lowercase(Locale.ROOT)
            if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
                val scheme = lowerUrl.substringBefore(":")
                invalidEntries.add(
                    HtmlInvalidEntry(
                        rawUrl = rawUrl,
                        title = rawTitle.takeIf { it.isNotEmpty() },
                        reason = "Unsupported protocol '$scheme' (only HTTP and HTTPS are permitted)"
                    )
                )
                return
            }

            if (!UrlUtils.isValidUrl(rawUrl)) {
                invalidEntries.add(
                    HtmlInvalidEntry(
                        rawUrl = rawUrl,
                        title = rawTitle.takeIf { it.isNotEmpty() },
                        reason = "Invalid URL format"
                    )
                )
                return
            }

            val normalized = runCatching { UrlUtils.normalizeUrl(rawUrl) }.getOrNull()
            if (normalized == null) {
                invalidEntries.add(
                    HtmlInvalidEntry(
                        rawUrl = rawUrl,
                        title = rawTitle.takeIf { it.isNotEmpty() },
                        reason = "URL normalization failed"
                    )
                )
                return
            }

            if (seenNormalizedUrls.contains(normalized)) {
                fileDuplicatesCount++
            } else {
                seenNormalizedUrls.add(normalized)
            }

            val title = if (rawTitle.isNotEmpty()) {
                rawTitle.take(500)
            } else {
                UrlUtils.extractDomain(rawUrl).ifEmpty { rawUrl }
            }

            val addDateSeconds = anchor.attr("add_date").toLongOrNull()
            val lastModifiedSeconds = anchor.attr("last_modified").toLongOrNull()

            val addDateMillis = addDateSeconds?.let { if (it > 0) it * 1000L else null }
            val modifiedMillis = lastModifiedSeconds?.let { if (it > 0) it * 1000L else null }

            val rawTags = anchor.attr("tags")
            val tagsList = if (rawTags.isNotBlank()) {
                rawTags.split(",")
                    .map { it.trim().take(50) }
                    .filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val cleanDescription = description?.trim()?.take(2000)?.ifEmpty { null }

            validBookmarks.add(
                HtmlBookmarkItem(
                    url = rawUrl,
                    normalizedUrl = normalized,
                    title = title,
                    folderPath = currentPath,
                    addedDate = addDateMillis,
                    modifiedDate = modifiedMillis,
                    tags = tagsList,
                    description = cleanDescription
                )
            )
        }

        fun traverseContainer(container: Element, currentPath: List<String>) {
            var pendingFolderName: String? = null
            var lastBookmarkIndex = -1

            for (child in container.children()) {
                val tag = child.tagName().lowercase(Locale.ROOT)
                when (tag) {
                    "dt" -> {
                        val h3 = child.selectFirst("h3")
                        val a = child.selectFirst("a")
                        val innerDl = child.selectFirst("dl")

                        if (h3 != null) {
                            val folderName = h3.text().trim().take(100)
                            if (folderName.isNotEmpty()) {
                                detectedFolders.add(folderName)
                                val newPath = currentPath + folderName
                                if (innerDl != null) {
                                    traverseContainer(innerDl, newPath)
                                } else {
                                    pendingFolderName = folderName
                                }
                            }
                        } else if (a != null) {
                            val innerDd = child.selectFirst("dd")?.text()?.trim()
                            val beforeCount = validBookmarks.size
                            processBookmarkAnchor(a, currentPath, innerDd)
                            if (validBookmarks.size > beforeCount) {
                                lastBookmarkIndex = validBookmarks.size - 1
                            }
                        }
                    }

                    "h3" -> {
                        val folderName = child.text().trim().take(100)
                        if (folderName.isNotEmpty()) {
                            detectedFolders.add(folderName)
                            pendingFolderName = folderName
                        }
                    }

                    "dl" -> {
                        val folderName = pendingFolderName
                        val newPath = if (!folderName.isNullOrEmpty()) {
                            currentPath + folderName
                        } else {
                            currentPath
                        }
                        traverseContainer(child, newPath)
                        pendingFolderName = null
                    }

                    "a" -> {
                        val beforeCount = validBookmarks.size
                        processBookmarkAnchor(child, currentPath, null)
                        if (validBookmarks.size > beforeCount) {
                            lastBookmarkIndex = validBookmarks.size - 1
                        }
                    }

                    "dd" -> {
                        val ddText = child.text().trim()
                        if (lastBookmarkIndex in validBookmarks.indices && validBookmarks[lastBookmarkIndex].description == null) {
                            val current = validBookmarks[lastBookmarkIndex]
                            validBookmarks[lastBookmarkIndex] = current.copy(
                                description = ddText.take(2000).ifEmpty { null }
                            )
                        }
                    }
                }
            }
        }

        traverseContainer(rootDl, emptyList())

        return HtmlParseResult(
            totalFound = validBookmarks.size + invalidEntries.size,
            validBookmarks = validBookmarks,
            invalidEntries = invalidEntries,
            detectedFolders = detectedFolders.toList(),
            fileDuplicateCount = fileDuplicatesCount
        )
    }
}
