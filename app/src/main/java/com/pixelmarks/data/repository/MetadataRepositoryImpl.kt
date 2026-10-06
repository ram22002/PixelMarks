package com.pixelmarks.data.repository

import com.pixelmarks.core.model.MetadataFetchResult
import com.pixelmarks.core.network.HtmlMetadataParser
import com.pixelmarks.core.network.HttpResponseResult
import com.pixelmarks.core.network.MetadataHttpClient

/**
 * Implementation of [MetadataRepository] coordinating HTTP retrieval and HTML parsing.
 */
class MetadataRepositoryImpl(
    private val httpClient: MetadataHttpClient,
    private val parser: HtmlMetadataParser = HtmlMetadataParser()
) : MetadataRepository {

    override suspend fun fetchMetadata(url: String): MetadataFetchResult {
        return when (val httpResult = httpClient.fetchHtml(url)) {
            is HttpResponseResult.Success -> {
                if (httpResult.html.isBlank()) {
                    MetadataFetchResult.NoMetadata
                } else {
                    val metadata = parser.parse(httpResult.html, httpResult.finalUrl)
                    // If no title, description, or custom image exists, mark as NO_METADATA
                    if (metadata.title.isNullOrBlank() &&
                        metadata.description.isNullOrBlank() &&
                        metadata.imageUrl.isNullOrBlank()
                    ) {
                        MetadataFetchResult.NoMetadata
                    } else {
                        MetadataFetchResult.Success(metadata)
                    }
                }
            }
            is HttpResponseResult.Blocked -> {
                MetadataFetchResult.Blocked(httpResult.reason)
            }
            is HttpResponseResult.Failed -> {
                MetadataFetchResult.Failed(httpResult.message, httpResult.statusCode)
            }
        }
    }
}
