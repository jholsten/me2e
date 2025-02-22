package org.jholsten.me2e.report.tracing.model

import org.jholsten.me2e.utils.prettyPrintJson
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Represents one HTTP trace in the network with ID [networkId], which
 * is composed of one [request] and one [response].
 */
data class AggregatedNetworkTrace internal constructor(

    /**
     * Unique identifier of this trace.
     */
    val id: UUID = UUID.randomUUID(),

    /**
     * ID of the network in which the request and response were captured.
     */
    val networkId: String,

    /**
     * ID of the parent trace that is superordinate to this trace.
     * If a server itself sends a request to another server to answer a request, the
     * second request is regarded as nested and receives the ID of the original trace
     * as its parent ID. The assignment of parents and children cannot be clearly
     * determined and is derived on the basis of the timestamps, IP addresses and ports
     * of the packets.
     */
    var parentId: UUID? = null,

    /**
     * Unique identifier of the stream which this trace is part of.
     * With nested requests/responses, all traces have the same [streamId].
     */
    var streamId: UUID = UUID.randomUUID(),

    /**
     * Client which sent the request. Is only set if IP address could be
     * associated with a corresponding container, Mock Server etc.
     */
    var client: NetworkNodeSpecification?,

    /**
     * Server which received the request. Is only set if IP address could be
     * associated with a corresponding container, Mock Server etc.
     */
    var server: NetworkNodeSpecification?,

    /**
     * Packet which represents the HTTP request of this trace.
     */
    val request: RequestPacket,

    /**
     * Packet which represents the HTTP response of this trace.
     */
    val response: ResponsePacket,
) {
    /**
     * Duration of the response in milliseconds, i.e. the time between request and response.
     */
    val duration: Long = Duration.between(request.timestamp, response.timestamp).toMillis()

    /**
     * Represents the information about one captured HTTP request packet.
     */
    class RequestPacket internal constructor(
        /**
         * Frame number of this packet.
         */
        val number: Int,

        /**
         * Timestamp of when this packet was sent.
         */
        val timestamp: Instant,

        /**
         * IP address of the host which has sent this packet.
         */
        val sourceIp: String,

        /**
         * Port of the application which has sent this packet.
         */
        val sourcePort: Int,

        /**
         * IP address of the host which has received this packet.
         */
        val destinationIp: String,

        /**
         * Port of the application which has received this packet.
         */
        val destinationPort: Int,

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
         * Request headers as a map of key and values.
         */
        val headers: Map<String, List<String>>,

        /**
         * Request body of the HTTP request.
         */
        val payload: Any?
    ) {
        /**
         * Status line of the HTTP request.
         */
        val statusLine: String = "$method $uri $version"

        internal constructor(packet: HttpRequestPacket) : this(
            number = packet.number,
            timestamp = packet.timestamp,
            sourceIp = packet.sourceIp,
            sourcePort = packet.sourcePort,
            destinationIp = packet.destinationIp,
            destinationPort = packet.destinationPort,
            version = packet.version,
            uri = packet.uri,
            method = packet.method,
            headers = packet.headers,
            payload = packet.payload?.let { if (it is String) prettyPrintJson(it) else it },
        )
    }

    /**
     * Represents the information about one captured HTTP response packet.
     */
    class ResponsePacket internal constructor(
        /**
         * Frame number of this packet.
         */
        val number: Int,

        /**
         * Timestamp of when this packet was sent.
         */
        val timestamp: Instant,

        /**
         * IP address of the host which has sent this packet.
         */
        val sourceIp: String,

        /**
         * Port of the application which has sent this packet.
         */
        val sourcePort: Int,

        /**
         * IP address of the host which has received this packet.
         */
        val destinationIp: String,

        /**
         * Port of the application which has received this packet.
         */
        val destinationPort: Int,

        /**
         * HTTP protocol version which was used for the HTTP response.
         */
        val version: String,

        /**
         * Response code of the HTTP response.
         */
        val statusCode: Int,

        /**
         * Description of the response code of the HTTP response.
         */
        val message: String,

        /**
         * Response headers as a map of key and values.
         */
        val headers: Map<String, List<String>>,

        /**
         * Response body of the HTTP response.
         */
        val payload: Any?
    ) {
        /**
         * Status line of the HTTP response.
         */
        val statusLine: String = "$version $statusCode $message"

        internal constructor(packet: HttpResponsePacket) : this(
            number = packet.number,
            timestamp = packet.timestamp,
            sourceIp = packet.sourceIp,
            sourcePort = packet.sourcePort,
            destinationIp = packet.destinationIp,
            destinationPort = packet.destinationPort,
            version = packet.version,
            statusCode = packet.statusCode,
            message = packet.message,
            headers = packet.headers,
            payload = packet.payload?.let { if (it is String) prettyPrintJson(it) else it },
        )
    }
}
