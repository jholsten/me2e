package org.jholsten.me2e.request.model

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
    val headers: Map<String, List<String>>,

    /**
     * Optional request body of this request.
     */
    val body: HttpRequestBody?,
)
