package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.utils.toJson
import java.time.Instant

/**
 * Represents the information about a captured HTTP request.
 */
internal class HttpRequestPacket(
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
     * HTTP protocol version which was used for the HTTP request.
     */
    version: String,

    /**
     * URI of the HTTP request.
     */
    val uri: String,

    /**
     * HTTP method of the HTTP request.
     */
    val method: String,

    /**
     * Request headers as a map of key and values.
     */
    headers: Map<String, List<String>>,

    /**
     * Request body of the HTTP request.
     */
    payload: Any?
) : HttpPacket(
    number = number,
    networkId = networkId,
    timestamp = timestamp,
    type = Type.REQUEST,
    sourceIp = sourceIp,
    sourcePort = sourcePort,
    destinationIp = destinationIp,
    destinationPort = destinationPort,
    version = version,
    headers = headers,
    payload = payload,
) {
    override fun toString(): String = toJson(this)
}
