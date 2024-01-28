package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents the information about a captured HTTP response.
 */
class HttpResponsePacket(
    /**
     * Frame number of this packet.
     */
    number: Int,

    /**
     * ID of the network in which this packet was captured.
     */
    @JsonProperty("network_id")
    networkId: String,

    /**
     * Timestamp of when this packet was sent.
     */
    timestamp: Instant,

    /**
     * IP address of the host which has sent this packet.
     */
    @JsonProperty("source_ip")
    sourceIp: String,

    /**
     * Port of the application which has sent this packet.
     */
    @JsonProperty("source_port")
    sourcePort: Int,

    /**
     * IP address of the host which has received this packet.
     */
    @JsonProperty("destination_ip")
    destinationIp: String,

    /**
     * Port of the application which has received this packet.
     */
    @JsonProperty("destination_port")
    destinationPort: Int,

    /**
     * HTTP protocol version which was used for the HTTP response.
     */
    version: String,

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
    headers: Map<String, String>,

    /**
     * Response body of the HTTP response.
     */
    payload: Any?
) : HttpPacket(
    number = number,
    networkId = networkId,
    timestamp = timestamp,
    type = Type.RESPONSE,
    sourceIp = sourceIp,
    sourcePort = sourcePort,
    destinationIp = destinationIp,
    destinationPort = destinationPort,
    version = version,
    headers = headers,
    payload = payload,
) {
    /**
     * Status line of the HTTP response.
     */
    val statusLine: String = "$version $statusCode $statusCodeDescription"
}
