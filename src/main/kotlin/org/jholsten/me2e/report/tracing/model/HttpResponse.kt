package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents the information about a captured HTTP response.
 */
data class HttpResponse(
    /**
     * HTTP protocol version which was used for the HTTP response.
     */
    val version: String,

    /**
     * Response code of the HTTP response.
     */
    @JsonProperty("status_code")
    val statusCode: Int,

    /**
     * Description of the response code of the HTTP response.
     */
    @JsonProperty("status_code_description")
    val statusCodeDescription: String,

    /**
     * Response headers as a map of key and value.
     */
    val headers: Map<String, String>,

    /**
     * Response body of the HTTP response.
     */
    val payload: Any?
) {
    /**
     * Status line of the HTTP response.
     */
    val statusLine: String = "$version $statusCode $statusCodeDescription"
}
