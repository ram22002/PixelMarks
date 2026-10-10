package com.pixelmarks.domain.usecase

import com.pixelmarks.core.model.html.HtmlExportResult
import com.pixelmarks.core.model.html.HtmlExportScope
import com.pixelmarks.core.model.html.HtmlImportOptions
import com.pixelmarks.core.model.html.HtmlImportPreview
import com.pixelmarks.core.model.html.HtmlImportSummary
import com.pixelmarks.core.model.html.HtmlParseResult
import com.pixelmarks.data.repository.HtmlBookmarkRepository
import java.io.InputStream
import java.io.OutputStream

class ParseBookmarksHtmlUseCase(
    private val htmlBookmarkRepository: HtmlBookmarkRepository
) {
    suspend operator fun invoke(inputStream: InputStream): HtmlParseResult {
        return htmlBookmarkRepository.parseHtml(inputStream)
    }
}

class GetHtmlImportPreviewUseCase(
    private val htmlBookmarkRepository: HtmlBookmarkRepository
) {
    suspend operator fun invoke(parseResult: HtmlParseResult, isPrivate: Boolean): HtmlImportPreview {
        return htmlBookmarkRepository.getImportPreview(parseResult, isPrivate)
    }
}

class ImportBookmarksHtmlUseCase(
    private val htmlBookmarkRepository: HtmlBookmarkRepository
) {
    suspend operator fun invoke(
        parseResult: HtmlParseResult,
        options: HtmlImportOptions,
        isPrivate: Boolean,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): HtmlImportSummary {
        return htmlBookmarkRepository.importBookmarks(
            parseResult = parseResult,
            options = options,
            isPrivate = isPrivate,
            onProgress = onProgress
        )
    }
}

class ExportBookmarksHtmlUseCase(
    private val htmlBookmarkRepository: HtmlBookmarkRepository
) {
    suspend operator fun invoke(
        scope: HtmlExportScope,
        isPrivate: Boolean,
        outputStream: OutputStream
    ): HtmlExportResult {
        return htmlBookmarkRepository.exportBookmarks(
            scope = scope,
            isPrivate = isPrivate,
            outputStream = outputStream
        )
    }
}
