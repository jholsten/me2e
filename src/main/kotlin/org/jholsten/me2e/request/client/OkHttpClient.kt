package org.jholsten.me2e.request.client

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Client to use for executing requests over HTTP.
 */
class HttpClient(
    /**
     * Base URL to set for all requests.
     */
    val baseUrl: String,
    /**
     * Configured client which executes the requests.
     */
    val httpClient: OkHttpClient = OkHttpClient(),
) {
    
    fun get(relativePath: String, headers: Map<String, String> = mapOf(), queryParams: Map<String, String> = mapOf()) {
        val request = Request.Builder().get()
        RequestBody.create("")
        httpClient.newCall()
    }
    
    private fun createUrlWithQueryParams(relativePath: String, queryParams: Map<String, String>): HttpUrl {
        val urlBuilder = createUrlBuilder(relativePath)
        for (param in queryParams) {
            urlBuilder.addQueryParameter(name = param.key, value = param.value)
        }
        return urlBuilder.build()
    }
    
    private fun createUrlBuilder(relativePath: String): HttpUrl.Builder {
        val url = (baseUrl + relativePath).toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid url format: '${baseUrl + relativePath}'")
        return url.newBuilder()
    }
}
