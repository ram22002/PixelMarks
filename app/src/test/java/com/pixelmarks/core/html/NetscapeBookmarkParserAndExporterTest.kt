package com.pixelmarks.core.html

import com.pixelmarks.core.model.Bookmark
import com.pixelmarks.core.model.Collection
import com.pixelmarks.core.model.html.HtmlExportScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class NetscapeBookmarkParserAndExporterTest {

    @Test
    fun testEmptyFileReturnsEmptyResult() {
        val stream = ByteArrayInputStream(ByteArray(0))
        val result = NetscapeBookmarkParser.parse(stream)
        assertEquals(0, result.totalFound)
        assertEquals(0, result.validBookmarks.size)
        assertEquals(0, result.invalidEntries.size)
    }

    @Test
    fun testRejectsNonHttpSchemes() {
        val html = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><A HREF="javascript:alert(1)">XSS Attempt</A>
                <DT><A HREF="file:///etc/passwd">Local File</A>
                <DT><A HREF="data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==">Data URI</A>
                <DT><A HREF="https://valid.com">Valid Bookmark</A>
            </DL><p>
        """.trimIndent()

        val result = NetscapeBookmarkParser.parse(ByteArrayInputStream(html.toByteArray()))
        assertEquals(4, result.totalFound)
        assertEquals(1, result.validBookmarks.size)
        assertEquals("https://valid.com", result.validBookmarks[0].url)
        assertEquals(3, result.invalidEntries.size)
        assertTrue(result.invalidEntries.any { it.rawUrl?.startsWith("javascript") == true })
        assertTrue(result.invalidEntries.any { it.rawUrl?.startsWith("file") == true })
        assertTrue(result.invalidEntries.any { it.rawUrl?.startsWith("data") == true })
    }

    @Test
    fun testHtmlEscapingAndUnicodeSpecialCharsRoundTrip() {
        val bookmarks = listOf(
            Bookmark(
                id = 1L,
                url = "https://example.com/search?q=a%20b&lang=en#heading_1",
                normalizedUrl = "https://example.com/search?q=a%20b&lang=en#heading_1",
                title = "Books & Comics \"Quotes\" <Special> 'Apos' 日本語 🔥",
                domain = "example.com",
                description = "Notes & <script>alert('safe')</script> & \"more\"",
                createdAt = 1700000000000L,
                updatedAt = 1700000005000L
            )
        )

        val collections = listOf(
            Collection(
                id = 10L,
                name = "Nested & <Safe> Folder 📁",
                createdAt = 1690000000000L,
                updatedAt = 1690000001000L
            )
        )

        val bookmarkInCollection = Bookmark(
            id = 2L,
            url = "https://github.com/developer/project",
            normalizedUrl = "https://github.com/developer/project",
            title = "Project Repo",
            domain = "github.com",
            collectionId = 10L,
            createdAt = 1700000010000L,
            updatedAt = 1700000020000L
        )

        val allBookmarks = bookmarks + bookmarkInCollection

        val tagsMap = mapOf(1L to listOf("reading", "japanese & tags", "fun"))

        val baos = ByteArrayOutputStream()
        val exportResult = NetscapeBookmarkExporter.export(
            bookmarks = allBookmarks,
            collections = collections,
            tagsMap = tagsMap,
            scope = HtmlExportScope.All,
            isPrivate = false,
            outputStream = baos
        )

        assertEquals(2, exportResult.exportedBookmarksCount)
        assertEquals(1, exportResult.exportedCollectionsCount)
        assertTrue(exportResult.bytesWritten > 0)

        val exportedHtml = baos.toString("UTF-8")
        assertTrue(exportedHtml.contains("&amp;"))
        assertTrue(exportedHtml.contains("&lt;Special&gt;"))
        assertTrue(exportedHtml.contains("&quot;Quotes&quot;"))
        assertTrue(exportedHtml.contains("日本語 🔥"))
        assertTrue(exportedHtml.contains("Nested &amp; &lt;Safe&gt; Folder 📁"))

        // Now parse the generated HTML back!
        val parseResult = NetscapeBookmarkParser.parse(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(2, parseResult.validBookmarks.size)
        assertEquals(0, parseResult.invalidEntries.size)

        val b1 = parseResult.validBookmarks.first { it.url.contains("example.com") }
        assertEquals("Books & Comics \"Quotes\" <Special> 'Apos' 日本語 🔥", b1.title)
        assertEquals(emptyList<String>(), b1.folderPath)
        assertEquals(listOf("reading", "japanese & tags", "fun"), b1.tags)
        // Description check (script tags text preserved without executing)
        assertNotNull(b1.description)
        assertTrue(b1.description!!.contains("alert('safe')"))

        val b2 = parseResult.validBookmarks.first { it.url.contains("github.com") }
        assertEquals(listOf("Nested & <Safe> Folder 📁"), b2.folderPath)
        assertEquals("Project Repo", b2.title)
    }

    @Test
    fun testNestedFolderHierarchyRoundTrip() {
        val rootCol = Collection(id = 1L, name = "Engineering", parentId = null)
        val childCol = Collection(id = 2L, name = "Mobile", parentId = 1L)
        val grandChildCol = Collection(id = 3L, name = "Android", parentId = 2L)

        val b = Bookmark(
            id = 100L,
            url = "https://developer.android.com",
            normalizedUrl = "https://developer.android.com",
            title = "Android Devs",
            domain = "developer.android.com",
            collectionId = 3L
        )

        val baos = ByteArrayOutputStream()
        NetscapeBookmarkExporter.export(
            bookmarks = listOf(b),
            collections = listOf(rootCol, childCol, grandChildCol),
            scope = HtmlExportScope.All,
            isPrivate = false,
            outputStream = baos
        )

        val parseResult = NetscapeBookmarkParser.parse(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, parseResult.validBookmarks.size)
        assertEquals(listOf("Engineering", "Mobile", "Android"), parseResult.validBookmarks[0].folderPath)
        assertEquals(listOf("Engineering", "Mobile", "Android"), parseResult.detectedFolders)
    }

    @Test
    fun testFileDuplicateUrlsCounted() {
        val html = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><A HREF="https://example.com/page">First</A>
                <DT><A HREF="https://example.com/page/">Second Duplicate With Slash</A>
                <DT><A HREF="https://different.com">Different</A>
            </DL><p>
        """.trimIndent()

        val parseResult = NetscapeBookmarkParser.parse(ByteArrayInputStream(html.toByteArray()))
        assertEquals(3, parseResult.totalFound)
        assertEquals(3, parseResult.validBookmarks.size)
        assertEquals(1, parseResult.fileDuplicateCount)
    }

    @Test
    fun testMissingTitleDefaultsToDomainOrUrl() {
        val html = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <DL><p>
                <DT><A HREF="https://news.ycombinator.com/item?id=123"></A>
            </DL><p>
        """.trimIndent()

        val parseResult = NetscapeBookmarkParser.parse(ByteArrayInputStream(html.toByteArray()))
        assertEquals(1, parseResult.validBookmarks.size)
        assertEquals("news.ycombinator.com", parseResult.validBookmarks[0].title)
    }

    @Test
    fun testCollectionSubtreeExport() {
        val col1 = Collection(id = 1L, name = "Col 1")
        val col2 = Collection(id = 2L, name = "Col 2")

        val b1 = Bookmark(id = 10L, url = "https://col1.com", normalizedUrl = "https://col1.com", title = "B1", domain = "col1.com", collectionId = 1L)
        val b2 = Bookmark(id = 20L, url = "https://col2.com", normalizedUrl = "https://col2.com", title = "B2", domain = "col2.com", collectionId = 2L)

        val baos = ByteArrayOutputStream()
        val result = NetscapeBookmarkExporter.export(
            bookmarks = listOf(b1, b2),
            collections = listOf(col1, col2),
            scope = HtmlExportScope.CollectionSubtree(1L, "Col 1"),
            isPrivate = false,
            outputStream = baos
        )

        assertEquals(1, result.exportedBookmarksCount)
        assertEquals(1, result.exportedCollectionsCount)
        val html = baos.toString("UTF-8")
        assertTrue(html.contains("https://col1.com"))
        assertFalse(html.contains("https://col2.com"))
    }
}
