package com.pixelmarks.core.network

/**
 * Result of an HTTP fetch for webpage content.
 */
sealed interface HttpResponseResult {
    data class Success(val html: String, val finalUrl: String) : HttpResponseResult
    data class Blocked(val reason: String) : HttpResponseResult
    data class Failed(val message: String, val statusCode: Int? = null) : HttpResponseResult
}

/**
 * Client interface for making anonymous, safe HTTP requests to retrieve webpage HTML.
 */
interface MetadataHttpClient {
    suspend fun fetchHtml(url: String): HttpResponseResult
}
