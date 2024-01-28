package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents one captured HTTP packet.
 */
open class HttpPacket(
    /**
     * Frame number of this packet.
     */
    val number: Int,

    /**
     * ID of the network in which this packet was captured.
     */
    @JsonProperty("network_id")
    val networkId: String,

    /**
     * Timestamp of when this packet was sent.
     */
    val timestamp: Instant,

    /**
     * IP address of the host which has sent this packet.
     */
    @JsonProperty("source_ip")
    val sourceIp: String,

    /**
     * Port of the application which has sent this packet.
     */
    @JsonProperty("source_port")
    val sourcePort: Int,

    /**
     * IP address of the host which has received this packet.
     */
    @JsonProperty("destination_ip")
    val destinationIp: String,

    /**
     * Port of the application which has received this packet.
     */
    @JsonProperty("destination_port")
    val destinationPort: Int,

    /**
     * Additional information about the HTTP request. Is only set
     * if this packet represents a request.
     */
    val request: Request?,

    /**
     * Additional information about the HTTP response. Is only set
     * if this packet represents a response.
     */
    val response: Response?
) {
    /**
     * Represents the information about a captured HTTP request.
     */
    data class Request(
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

    /**
     * Represents the information about a captured HTTP response.
     */
    data class Response(
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
         * Frame number of the request to which this response corresponds.
         */
        @JsonProperty("request_in")
        val requestIn: Int?,

        /**
         * Duration of the response in seconds, i.e. the time since the request.
         */
        val duration: Float?,

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
}
