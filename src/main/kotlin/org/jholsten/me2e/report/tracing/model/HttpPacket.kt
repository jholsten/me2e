package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Represents one captured HTTP packet.
 */
data class HttpPacket(
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
    val request: HttpRequest?,

    /**
     * Additional information about the HTTP response. Is only set
     * if this packet represents a response.
     */
    val response: HttpResponse?
)
