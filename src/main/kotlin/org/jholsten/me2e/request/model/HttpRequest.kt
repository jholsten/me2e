package org.jholsten.me2e.request.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.internal.toImmutableMap
import org.apache.commons.lang3.StringUtils

/**
 * Model representing an HTTP request.
 */
class HttpRequest internal constructor(
    /**
     * URL of this request.
     */
    val url: String,

    /**
     * HTTP method of this request.
     */
    val method: HttpMethod,

    /**
     * Headers of this request.
     */
    val headers: Map<String, List<String>> = mapOf(),

    /**
     * Optional request body of this request.
     */
    val body: HttpRequestBody? = null,
) {
    /**
     * Returns new builder instance initialized with the properties of this HTTP request.
     * This allows to create new instances with partly modified properties.
     */
    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder constructor() {
        private var url: String? = null
        private var method: HttpMethod? = null
        private var headers: MutableMap<String, List<String>> = mutableMapOf()
        private var body: HttpRequestBody? = null

        internal constructor(httpRequest: HttpRequest) : this() {
            this.url = httpRequest.url
            this.method = httpRequest.method
            this.headers = httpRequest.headers.toMutableMap()
            this.body = httpRequest.body
        }

        /**
         * Builds the URL from base URL, relative path and optional query parameters.
         * Example:
         * ```
         * withUrl(baseUrl="https://google.com", relativePath="/search", queryParams=mapOf("name" to "dog")
         * // Generates: "https://google.com/search?name=dog"
         * ```
         * The base URL and relative path are normalized, so that they can include leading and trailing
         * slashes, which will be removed when constructing the URL.
         */
        fun withUrl(baseUrl: String, relativePath: String, queryParams: Map<String, String> = mapOf()) = apply {
            val normalizedBaseUrl = StringUtils.stripEnd(baseUrl, "/")
            val normalizedRelativePath = StringUtils.stripStart(relativePath, "/")
            val url = "$normalizedBaseUrl/$normalizedRelativePath"
            val urlBuilder = url.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid url format: '$url'")
            for (param in queryParams) {
                urlBuilder.addQueryParameter(name = param.key, value = param.value)
            }

            this.url = urlBuilder.build().toString()
        }

        fun withMethod(method: HttpMethod) = apply {
            this.method = method
        }

        fun withHeaders(headers: Map<String, List<String>>) = apply {
            this.headers = headers.toMutableMap()
        }

        fun addHeader(key: String, value: String) = apply {
            val values = this.headers.getOrDefault(key, listOf()).toMutableList()
            if (!values.contains(value)) {
                values.add(value)
                this.headers[key] = values
            }
        }

        fun withBody(body: HttpRequestBody?) = apply {
            this.body = body
        }

        fun build(): HttpRequest {
            val url = this.url ?: throw IllegalArgumentException("Url cannot be null")
            val method = this.method ?: throw IllegalArgumentException("Http method cannot be null")
            if (method.requiresRequestBody()) {
                require(body != null) { "HTTP method $method needs to have a request body" }
            }
            if (!method.allowsRequestBody()) {
                require(body == null) { "HTTP method $method does not allow to have a request body" }
            }
            return HttpRequest(
                url = url,
                method = method,
                headers = headers.toImmutableMap(),
                body = body,
            )
        }
    }
}
