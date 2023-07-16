package org.jholsten.me2e.request.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.apache.commons.lang3.StringUtils

/**
 * Model representing an HTTP request.
 */
class HttpRequest(
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
    val body: HttpRequestBody?,
) {
    class Builder {
        private var url: String? = null
        private var method: HttpMethod? = null
        private var headers: Map<String, List<String>> = mapOf()
        private var body: HttpRequestBody? = null

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
        fun withUrl(baseUrl: String, relativePath: String, queryParams: Map<String, String> = mapOf()): Builder {
            val normalizedBaseUrl = StringUtils.stripEnd(baseUrl, "/")
            val normalizedRelativePath = StringUtils.stripStart(relativePath, "/")
            val url = "$normalizedBaseUrl/$normalizedRelativePath"
            val urlBuilder = url.toHttpUrlOrNull()?.newBuilder() ?: throw IllegalArgumentException("Invalid url format: '$url'")
            for (param in queryParams) {
                urlBuilder.addQueryParameter(name = param.key, value = param.value)
            }

            this.url = urlBuilder.build().toString()
            return this
        }

        fun withMethod(method: HttpMethod): Builder {
            this.method = method
            return this
        }

        fun withHeaders(headers: Map<String, List<String>>): Builder {
            this.headers = headers
            return this
        }

        fun withBody(body: HttpRequestBody?): Builder {
            this.body = body
            return this
        }

        fun build(): HttpRequest {
            val url = this.url ?: throw IllegalArgumentException("Url cannot be null")
            val method = this.method ?: throw IllegalArgumentException("Http method cannot be null")
            return HttpRequest(
                url = url,
                method = method,
                headers = headers,
                body = body,
            )
        }
    }
}
