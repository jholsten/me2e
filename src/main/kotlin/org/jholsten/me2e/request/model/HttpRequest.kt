package org.jholsten.me2e.request.model

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
    val headers: HttpHeaders = HttpHeaders.empty(),

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
        private var headerBuilder: HttpHeaders.Builder = HttpHeaders.Builder()
        private var body: HttpRequestBody? = null

        internal constructor(httpRequest: HttpRequest) : this() {
            this.url = httpRequest.url
            this.method = httpRequest.method
            this.headerBuilder = httpRequest.headers.newBuilder()
            this.body = httpRequest.body
        }

        /**
         * Sets the given absolute URL for the request.
         * @param url URL of the request to set.
         * @return Builder instance, to use for chaining.
         */
        fun withUrl(url: Url) = apply {
            this.url = url
        }

        /**
         * Sets the given HTTP request method for the request.
         * @param method HTTP request method to set.
         * @return Builder instance, to use for chaining.
         */
        fun withMethod(method: HttpMethod) = apply {
            this.method = method
        }

        /**
         * Sets the given HTTP headers for the request.
         * @param headers HTTP headers to set.
         * @return Builder instance, to use for chaining.
         */
        fun withHeaders(headers: HttpHeaders) = apply {
            this.headerBuilder = headers.newBuilder()
        }

        /**
         * Adds an HTTP header with the given [key] and the given [value].
         * @param key Key of the header to add.
         * @param value Value of the header to add.
         * @return Builder instance, to use for chaining.
         */
        fun addHeader(key: String, value: String) = apply {
            this.headerBuilder.add(key, value)
        }

        /**
         * Sets the given request body for the request.
         * @param body Request body to set.
         * @return Builder instance, to use for chaining.
         */
        fun withBody(body: HttpRequestBody?) = apply {
            this.body = body
        }

        /**
         * Builds an instance of the [HttpRequest] using the properties set in this builder.
         */
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
                headers = headerBuilder.build(),
                body = body,
            )
        }
    }
}
