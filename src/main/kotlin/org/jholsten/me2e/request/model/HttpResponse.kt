package org.jholsten.me2e.request.model

/**
 * Model representing an HTTP response.
 */
class HttpResponse(
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
)
