package org.jholsten.me2e.report.tracing.model

/**
 * Represents the information about a captured HTTP request.
 */
data class HttpRequest(
    /**
     * HTTP protocol version which was used for the HTTP request.
     */
    val version: String,

    /**
     * URI of the HTTP request.
     */
    val uri: String,

    /**
     * HTTP method of the HTTP request.
     */
    val method: String,

    /**
     * Request headers as a map of key and value.
     */
    val headers: Map<String, String>,

    /**
     * Request body of the HTTP request.
     */
    val payload: Any?
) {
    /**
     * Status line of the HTTP request.
     */
    val statusLine: String = "$method $uri $version"
}
