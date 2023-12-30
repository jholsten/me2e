package org.jholsten.me2e.request.model

import okhttp3.internal.toImmutableMap

/**
 * Model representing an HTTP request.
 */
class HttpRequest internal constructor(
    /**
     * URL of this request.
     */
    val url: Url,

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

    class Builder() {
        private var url: Url? = null
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
         * Sets the given absolute URL for this request.
         */
        fun withUrl(url: Url) = apply {
            this.url = url
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
