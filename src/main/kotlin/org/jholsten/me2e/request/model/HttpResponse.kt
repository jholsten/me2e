package org.jholsten.me2e.request.model

import okhttp3.internal.toImmutableMap

/**
 * Model representing an HTTP response.
 */
class HttpResponse internal constructor(
    /**
     * The request that initiated this HTTP response.
     * This may be different to the original request executed by the application
     * (e.g. in case the request was redirected).
     */
    val request: HttpRequest,

    /**
     * HTTP protocol that was used, such as `http/1.1`.
     */
    val protocol: String,

    /**
     * HTTP status message of this response.
     */
    val message: String,

    /**
     * HTTP status code of this response.
     */
    val code: Int,

    /**
     * HTTP headers set in this response.
     */
    val headers: Map<String, List<String>> = mapOf(),

    /**
     * Response body of this response.
     */
    val body: HttpResponseBody?,
) {

    /**
     * Returns new builder instance initialized with the properties of this HTTP response.
     * This allows to create new instances with partly modified properties.
     */
    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder constructor() {
        private var request: HttpRequest? = null
        private var protocol: String? = null
        private var message: String? = null
        private var code: Int? = null
        private var headers: MutableMap<String, List<String>> = mutableMapOf()
        private var body: HttpResponseBody? = null

        internal constructor(httpResponse: HttpResponse) : this() {
            this.request = httpResponse.request
            this.protocol = httpResponse.protocol
            this.message = httpResponse.message
            this.code = httpResponse.code
            this.headers = httpResponse.headers.toMutableMap()
            this.body = httpResponse.body
        }

        fun withRequest(request: HttpRequest) = apply {
            this.request = request
        }

        fun withProtocol(protocol: String) = apply {
            this.protocol = protocol
        }

        fun withMessage(message: String) = apply {
            this.message = message
        }

        fun withCode(code: Int) = apply {
            this.code = code
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

        fun withBody(body: HttpResponseBody?) = apply {
            this.body = body
        }

        fun build(): HttpResponse {
            val request = this.request ?: throw IllegalArgumentException("Request cannot be null")
            val protocol = this.protocol ?: throw IllegalArgumentException("Protocol cannot be null")
            val message = this.message ?: throw IllegalArgumentException("Message cannot be null")
            val code = this.code ?: throw IllegalArgumentException("Code cannot be null")
            return HttpResponse(
                request = request,
                protocol = protocol,
                message = message,
                code = code,
                headers = headers.toImmutableMap(),
                body = body,
            )
        }
    }
}
