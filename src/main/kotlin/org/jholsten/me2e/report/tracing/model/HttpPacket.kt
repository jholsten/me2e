package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

/**
 * Representation of one captured HTTP packet. May be either a request or a response.
 * Corresponds to the DTO which is transferred by the [docker-traffic-capturer](https://gitlab.informatik.uni-bremen.de/jholsten/docker-traffic-capturer).
 * @see HttpRequestPacket
 * @see HttpResponsePacket
 * @see <a href="https://gitlab.informatik.uni-bremen.de/jholsten/docker-traffic-capturer">docker-traffic-capturer</a>
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.PROPERTY,
    visible = true,
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = HttpRequestPacket::class, name = "REQUEST"),
        JsonSubTypes.Type(value = HttpResponsePacket::class, name = "RESPONSE"),
    ]
)
internal abstract class HttpPacket(
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
     * Type of this HTTP packet.
     */
    val type: Type,

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
     * HTTP protocol version which was used.
     */
    val version: String,

    /**
     * Request/Response headers as a map of key and values.
     */
    val headers: Map<String, List<String>>,

    /**
     * Request/Response body of this packet.
     */
    val payload: Any?,
) {
    enum class Type { REQUEST, RESPONSE }
}
