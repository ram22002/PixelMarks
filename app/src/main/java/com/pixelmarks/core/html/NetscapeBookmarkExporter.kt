package com.pixelmarks.core.html

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.html.HtmlExportResult
import com.pixelmarks.core.model.html.HtmlExportScope
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * Standards-compliant exporter for Netscape Bookmark HTML.
 * Generates browser-compatible UTF-8 HTML with hierarchical folders, HTML escaping,
 * timestamps, and tags.
 */
object NetscapeBookmarkExporter {

    fun escapeHtml(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return buildString(text.length) {
            for (ch in text) {
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(ch)
                }
            }
        }
    }

    /**
     * Exports bookmarks and collections to the provided OutputStream in Netscape Bookmark HTML format.
     */
    fun export(
        bookmarks: List<Bookmark>,
        collections: List<Collection>,
        tagsMap: Map<Long, List<String>> = emptyMap(),
        scope: HtmlExportScope = HtmlExportScope.All,
        isPrivate: Boolean,
        outputStream: OutputStream
    ): HtmlExportResult {
        var exportedBookmarksCount = 0
        var exportedCollectionsCount = 0
        var totalBytesWritten = 0L

        val countingOutputStream = object : OutputStream() {
            override fun write(b: Int) {
                outputStream.write(b)
                totalBytesWritten++
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                outputStream.write(b, off, len)
                totalBytesWritten += len
            }

            override fun flush() {
                outputStream.flush()
            }
        }

        val writer = BufferedWriter(OutputStreamWriter(countingOutputStream, StandardCharsets.UTF_8))

        writer.write("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        writer.write("<!-- This is an automatically generated file.\n")
        writer.write("     It will be read and overwritten.\n")
        writer.write("     DO NOT EDIT! -->\n")
        writer.write("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        writer.write("<TITLE>Bookmarks</TITLE>\n")
        writer.write("<H1>Bookmarks</H1>\n")
        writer.write("<DL><p>\n")

        val collectionsById = collections.associateBy { it.id }
        val childCollectionsByParent = collections.groupBy { it.parentId }
        val bookmarksByCollection = bookmarks.groupBy { it.collectionId }

        fun writeBookmark(bookmark: Bookmark, indent: String) {
            val addDate = bookmark.createdAt / 1000L
            val modDate = bookmark.updatedAt / 1000L
            val tags = tagsMap[bookmark.id]?.filter { it.isNotBlank() } ?: emptyList()
            val tagsAttr = if (tags.isNotEmpty()) {
                val tagsString = escapeHtml(tags.joinToString(","))
                " TAGS=\"$tagsString\""
            } else {
                ""
            }

            val escapedUrl = escapeHtml(bookmark.url)
            val escapedTitle = escapeHtml(bookmark.title.ifEmpty { bookmark.domain })
            writer.write("$indent<DT><A HREF=\"$escapedUrl\" ADD_DATE=\"$addDate\" LAST_MODIFIED=\"$modDate\"$tagsAttr>$escapedTitle</A>\n")

            val desc = bookmark.description?.ifBlank { null } ?: bookmark.notes?.ifBlank { null }
            if (desc != null) {
                val escapedDesc = escapeHtml(desc)
                writer.write("$indent<DD>$escapedDesc\n")
            }
            exportedBookmarksCount++
        }

        fun writeCollectionTree(collection: Collection, indent: String) {
            val addDate = collection.createdAt / 1000L
            val modDate = collection.updatedAt / 1000L
            val escapedName = escapeHtml(collection.name)

            writer.write("$indent<DT><H3 ADD_DATE=\"$addDate\" LAST_MODIFIED=\"$modDate\">$escapedName</H3>\n")
            writer.write("$indent<DL><p>\n")
            exportedCollectionsCount++

            val nextIndent = "$indent    "

            // Write bookmarks in this collection
            bookmarksByCollection[collection.id]?.forEach { b ->
                writeBookmark(b, nextIndent)
            }

            // Write child collections
            childCollectionsByParent[collection.id]?.forEach { childCol ->
                writeCollectionTree(childCol, nextIndent)
            }

            writer.write("$indent</DL><p>\n")
        }

        when (scope) {
            is HtmlExportScope.All -> {
                // First write uncollected bookmarks at root level
                bookmarksByCollection[null]?.forEach { b ->
                    writeBookmark(b, "    ")
                }

                // Any bookmark referencing non-existent collections also at root
                bookmarks.filter { it.collectionId != null && !collectionsById.containsKey(it.collectionId) }.forEach { b ->
                    writeBookmark(b, "    ")
                }

                // Write root collections
                childCollectionsByParent[null]?.forEach { rootCol ->
                    writeCollectionTree(rootCol, "    ")
                }
            }

            is HtmlExportScope.CollectionSubtree -> {
                val targetCol = collectionsById[scope.collectionId]
                if (targetCol != null) {
                    writeCollectionTree(targetCol, "    ")
                }
            }
        }

        writer.write("</DL><p>\n")
        writer.flush()

        return HtmlExportResult(
            exportedBookmarksCount = exportedBookmarksCount,
            exportedCollectionsCount = exportedCollectionsCount,
            bytesWritten = totalBytesWritten,
            isPrivate = isPrivate
        )
    }
}
