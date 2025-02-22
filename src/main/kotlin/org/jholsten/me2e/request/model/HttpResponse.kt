package org.jholsten.me2e.request.model

/**
 * Model representing an HTTP response.
 */
class HttpResponse internal constructor(
    /**
     * The request that initiated this HTTP response.
     * This may be different to the original request executed by the application (e.g. in case the request was
     * redirected or modified by a request interceptor).
     */
    val request: HttpRequest,

    /**
     * HTTP protocol version that was used, such as `HTTP/1.1`.
     */
    val protocol: String,

    /**
     * HTTP status message of this response.
     */
    val message: String,

    /**
     * HTTP status code of this response.
     */
    val statusCode: Int,

    /**
     * HTTP headers set in this response.
     */
    val headers: HttpHeaders = HttpHeaders.empty(),

    /**
     * Response body of this response.
     */
    val body: HttpResponseBody? = null,
) {
    override fun toString(): String {
        return "$protocol $statusCode $message"
    }
}
