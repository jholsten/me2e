package org.jholsten.me2e.report.tracing.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import java.time.Instant

/**
 * Represents one captured HTTP packet, for which the source and destination IP addresses
 * are associated with the corresponding services. Also contains a reference to the parent
 * of this packet.
 */
class AggregatedHttpPacket(
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
     * Service which has sent this packet. Is only set if [sourceIp] and [sourcePort]
     * could be associated to a corresponding service.
     */
    val source: ServiceSpecification?,

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
     * Service which has received this packet. Is only set if [destinationIp] and [destinationPort]
     * could be associated to a corresponding service.
     */
    val destination: ServiceSpecification?,

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
     * Additional information about the HTTP request. Is only set
     * if this packet represents a request.
     */
    request: Request?,

    /**
     * Additional information about the HTTP response. Is only set
     * if this packet represents a response.
     */
    response: Response?
) : HttpPacket(
    number = number,
    networkId = networkId,
    timestamp = timestamp,
    sourceIp = sourceIp,
    sourcePort = sourcePort,
    destinationIp = destinationIp,
    destinationPort = destinationPort,
    request = request,
    response = response,
)
