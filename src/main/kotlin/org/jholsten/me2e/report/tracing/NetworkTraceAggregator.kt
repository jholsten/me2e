package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.Me2eTest
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.network.ContainerNetwork
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.report.result.model.FinishedTestResult
import org.jholsten.me2e.report.tracing.model.*
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.ToStringConsumer
import java.lang.Exception
import java.time.Instant

/**
 * Service which aggregates all HTTP packets sent in the Docker networks, which the containers are part of.
 * Enables to trace requests and responses. Matches IP addresses to the associated services.
 */
internal class NetworkTraceAggregator {
    companion object {
        private val logger = logger<NetworkTraceAggregator>()

        /**
         * IP address of the host which is running the tests.
         * If the environment variable `RUNNER_IP` is not set, it is assumed that the host which is running the tests
         * is the same as the Docker host.
         */
        private val hostIpAddress: String by lazy {
            val ipAddress = System.getenv("RUNNER_IP")?.trim() ?: getHostDockerInternalIp()
            logger.info("Host IP address is $ipAddress.")
            ipAddress
        }

        /**
         * Starts a temporary Docker container to retrieve the IP address of `host.docker.internal`,
         * i.e. the IP address of the Docker host.
         * @return IP address of `host.docker.internal`.
         */
        private fun getHostDockerInternalIp(): String {
            try {
                val toStringConsumer = ToStringConsumer()
                val tempContainer = GenericContainer("alpine")
                    .withCommand("sh", "-c", "getent hosts host.docker.internal | awk '{ print $1 }'")
                    .withExtraHost("host.docker.internal", "host-gateway")
                    .withLogConsumer(toStringConsumer)
                tempContainer.start()

                val ipAddress = toStringConsumer.toUtf8String().replace("\n", "")
                if (ipAddress.isNotBlank()) {
                    return ipAddress
                }
            } catch (e: Exception) {
                logger.warn("Exception occurred while trying to retrieve IP address of `host.docker.internal`: $e")
            }
            logger.warn("Unable to determine IP address of `host.docker.internal`. Will be using default address `192.168.65.2` to match packets to host.")
            return "192.168.65.2"
        }
    }

    private val logger = logger<NetworkTraceAggregator>()

    /**
     * Docker network IDs for which the HTTP traffic is captured along with the Docker containers in the
     * networks and their IP addresses as a map of network IDs and (ip, container).
     */
    private val monitoredNetworks: MutableMap<String, MutableMap<String, NetworkNodeSpecification>> = mutableMapOf()

    /**
     * Network IDs along with their gateway. Includes only networks which contain a gateway, i.e. networks
     * which are accessible from outside.
     */
    private val networkGateways: MutableMap<String, NetworkNodeSpecification> = mutableMapOf()

    /**
     * Network IDs along with a corresponding trace collector which is responsible for collecting all
     * network traffic inside this network.
     */
    private val networkTraceCollectors: MutableMap<String, NetworkTraceCollector> = mutableMapOf()

    /**
     * Node representation of all Mock Servers in the test environment.
     * The Mock Servers are not actually separate nodes in the network, but are reachable via the Test Runner on port 80/443.
     * Requests to the Mock Servers are assigned via the name specified in the host header.
     */
    private val mockServers: Map<String, NetworkNodeSpecification> by lazy {
        Me2eTest.config.environment.mockServers.values
            .associate {
                it.hostname to NetworkNodeSpecification(
                    nodeType = NetworkNodeSpecification.NodeType.MOCK_SERVER,
                    ipAddress = hostIpAddress,
                    specification = ServiceSpecification(name = it.name),
                )
            }
    }

    /**
     * Network node representing the test runner.
     */
    private val testRunner: NetworkNodeSpecification by lazy {
        NetworkNodeSpecification(
            nodeType = NetworkNodeSpecification.NodeType.TEST_RUNNER,
            ipAddress = hostIpAddress,
            specification = ReportDataAggregator.testRunner,
        )
    }

    /**
     * Initializes collectors for capturing HTTP traffic in the networks of the given [container].
     * Registers the container with its IP address and instantiates a network trace collector for
     * each of its networks, if this network is not already being monitored.
     * @param container Container which has been started and for which the network traffic should be captured.
     * @param specification Representation of the [container] instance.
     */
    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        for ((networkName, network) in container.networks) {
            try {
                initializeNetworkTraceCollector(networkName, network)
            } catch (e: Exception) {
                logger.warn(
                    "Unable to initialize network trace collector for network $network. Cannot capture HTTP traffic for this network.",
                    e
                )
                continue
            }
            registerContainer(network, container, specification)
        }
        logger.info("Initialized network trace aggregator for container ${container.name}.")
    }

    /**
     * Collects and aggregates all captured packets in all networks.
     * Associates packets with the corresponding tests by their timestamps.
     * @param roots Roots of the test execution tree.
     */
    @JvmSynthetic
    internal fun collectPackets(roots: List<FinishedTestResult>) {
        if (networkTraceCollectors.isEmpty()) return
        logger.info("Collecting packets...")
        val packets: MutableList<HttpPacket> = mutableListOf()
        for (monitor in networkTraceCollectors.values) {
            packets.addAll(monitor.collect())
        }
        logger.info("Collected ${packets.size} HTTP packets from all networks.")
        val aggregatedTraces = aggregateTraces(packets)
        val testTraces = matchTracesToTests(roots, aggregatedTraces)
        for ((test, traces) in testTraces) {
            test.traces = associateStreams(traces.toMutableList())
        }
        reviseClients(aggregatedTraces)
    }

    /**
     * Aggregates the given packets into [AggregatedNetworkTrace] instances, each of which represents an
     * HTTP request along with the associated HTTP response. Matches source and destination IP addresses
     * to the corresponding network nodes.
     * @param packets All HTTP packets captured in all registered networks.
     */
    private fun aggregateTraces(packets: MutableList<HttpPacket>): MutableList<AggregatedNetworkTrace> {
        val requestResponses: Map<HttpRequestPacket, HttpResponsePacket> = findRequestResponsePairs(packets)
        val aggregatedTraces: MutableList<AggregatedNetworkTrace> = mutableListOf()
        for ((requestPacket, responsePacket) in requestResponses) {
            val (client, server) = matchSourceAndDestination(requestPacket)
            val aggregatedRequest = AggregatedNetworkTrace.RequestPacket(requestPacket)
            val aggregatedResponse = AggregatedNetworkTrace.ResponsePacket(responsePacket)
            aggregatedTraces.add(
                AggregatedNetworkTrace(
                    networkId = requestPacket.networkId,
                    client = client,
                    server = server,
                    request = aggregatedRequest,
                    response = aggregatedResponse,
                )
            )
        }
        return aggregatedTraces
    }

    /**
     * Groups the given traces into streams, which can consist of several nested requests, for which further requests were sent
     * in response to the original request. Considers the timestamps and IP addresses of the requests to identify nested requests.
     * @return List of traces, sorted by streams and timestamp of the first request.
     */
    private fun associateStreams(aggregatedTraces: MutableList<AggregatedNetworkTrace>): List<AggregatedNetworkTrace> {
        aggregatedTraces.sortByDescending { it.response.timestamp }
        for (trace in aggregatedTraces) {
            val tracesInBetween = aggregatedTraces.findTracesInBetween(trace.request, trace.response)
            for (traceInBetween in tracesInBetween) {
                if (traceInBetween.parentId == null && traceInBetween.request.sourceIp == trace.request.destinationIp) {
                    traceInBetween.parentId = trace.id
                    traceInBetween.streamId = trace.streamId
                }
            }
        }
        val streams = aggregatedTraces.groupBy { it.streamId }
            .map { it.key to it.value.sortedBy { trace -> trace.request.timestamp } }
            .sortedBy { it.second.first().request.timestamp }
        return streams.flatMap { it.second }
    }

    /**
     * Finds all pairs of request and response packet in the given list of all captured packets.
     * If no corresponding request can be found for a response, it is ignored.
     * @return Map of all associated request and response pairs.
     * @param packets All HTTP packets captured in all registered networks.
     */
    private fun findRequestResponsePairs(packets: MutableList<HttpPacket>): Map<HttpRequestPacket, HttpResponsePacket> {
        packets.sortBy { it.timestamp }
        val responsePackets = packets.filterIsInstance<HttpResponsePacket>()
        val requestResponses: MutableMap<HttpRequestPacket, HttpResponsePacket> = mutableMapOf()
        for (responsePacket in responsePackets) {
            val requestPacket = findCorrespondingRequest(responsePacket, packets)
            if (requestPacket == null) {
                logger.warn("Unable to find corresponding request for response $responsePacket.")
                continue
            }
            requestResponses[requestPacket] = responsePacket
        }
        return requestResponses
    }

    /**
     * Tries to find the corresponding request for the given response in the list of all [packets].
     * For response packets which have not left their network, TShark sets the frame number of the corresponding request in the
     * response packet as [HttpResponsePacket.requestIn]. In this case, the request with this frame number is returned.
     * In contrast, responses that are directed to a host outside the network, are sent by Docker to the loopback address
     * `127.0.0.1` and TShark cannot associate the response with the request, as the ACK number does not match the sequence
     * number of the request. Therefore, in this case a request is searched for that fulfills the following conditions:
     * - The timestamp is before that of the response
     * - The request was sent via the network gateway
     * - The source port of the request corresponds to the destination port of the response
     * @param responsePacket Packet for which corresponding request is to be found.
     * @param packets List of all captured packets, sorted by their timestamp.
     * @return Corresponding request for the given response.
     */
    private fun findCorrespondingRequest(responsePacket: HttpResponsePacket, packets: List<HttpPacket>): HttpRequestPacket? {
        val networkPackets = packets.filter { it.networkId == responsePacket.networkId }
        if (responsePacket.requestIn != null) {
            return networkPackets.filterIsInstance<HttpRequestPacket>().find { it.number == responsePacket.requestIn }
        }
        if (responsePacket.destinationIp == "127.0.0.1") {
            /*
            Responses to hosts outside the network are sent to the loopback address 127.0.0.1 and are then
            forwarded by Docker to the receiving host via the corresponding virtual ethernet interface.
            Therefore, we need to find the last request that came into this network via its gateway.
             */
            val index = networkPackets.indexOf(responsePacket)
            for (i in index downTo 0) {
                val packet = networkPackets[i]
                val networkGateway = networkGateways[responsePacket.networkId]?.ipAddress
                if (packet is HttpRequestPacket && packet.sourceIp == networkGateway && packet.sourcePort == responsePacket.destinationPort) {
                    return packet
                }
            }
        }
        return null
    }

    /**
     * Matches the source and destination IP address of the given request packet to the nodes in the network.
     * If no such node can be found, `null` is returned.
     * @return Pair of source node and destination node.
     */
    private fun matchSourceAndDestination(packet: HttpRequestPacket): Pair<NetworkNodeSpecification?, NetworkNodeSpecification?> {
        val source = matchIpAndPort(packet.networkId, packet.sourceIp, packet.sourcePort, packet)
        val destination = matchIpAndPort(packet.networkId, packet.destinationIp, packet.destinationPort, packet)
        return source to destination
    }

    /**
     * Tries to find the node in the network which is associated with the given IP address.
     * If the IP address is not found in the list of registered containers, it may be the test runner,
     * a network gateway or one of the Mock Servers which sent/received the associated packet.
     */
    private fun matchIpAndPort(networkId: String, ipAddress: String, port: Int, packet: HttpRequestPacket): NetworkNodeSpecification? {
        val networkContainers = monitoredNetworks[networkId] ?: return null
        if (ipAddress in networkContainers) {
            return networkContainers[ipAddress]
        }

        if (ipAddress == networkGateways[networkId]?.ipAddress) {
            return networkGateways[networkId]
        }

        if (ipAddress == hostIpAddress) {
            if ((port == 80 || port == 443) && mockServers.isNotEmpty()) {
                return findMockServer(packet)
            }
            return testRunner
        }
        logger.warn("Unable to find network node for IP address $ipAddress in network $networkId.")
        return null
    }

    /**
     * Matches all traces to the corresponding test by their timestamps.
     * Returns map of assigned tests and their associated traces.
     */
    private fun matchTracesToTests(
        roots: List<FinishedTestResult>,
        traces: List<AggregatedNetworkTrace>,
    ): Map<FinishedTestResult, List<AggregatedNetworkTrace>> {
        if (traces.isEmpty()) {
            return mapOf()
        }
        val testTraces: MutableMap<FinishedTestResult, List<AggregatedNetworkTrace>> = mutableMapOf()
        for (test in roots) {
            matchTracesToTest(test, traces, testTraces)
        }
        return testTraces
    }

    /**
     * Matches network traces to the given test according to their timestamps.
     * @param test Test for which corresponding traces are to be matched.
     * @param traces All traces captured from registered all networks.
     * @param testTraces Tests with their associated traces assigned so far.
     */
    private fun matchTracesToTest(
        test: FinishedTestResult,
        traces: List<AggregatedNetworkTrace>,
        testTraces: MutableMap<FinishedTestResult, List<AggregatedNetworkTrace>>,
    ) {
        testTraces[test] = traces.findTracesBetween(test.startTime, test.endTime)

        val finishedChildren = test.children.filterIsInstance<FinishedTestResult>()
        for (child in finishedChildren) {
            matchTracesToTest(child, traces, testTraces)
        }
    }

    /**
     * After all streams have been aggregated and the client and server nodes have been assigned based on the IP addresses,
     * the assigned clients are revised using this method. For requests received via the network gateway that are not part
     * of a nested request, it is assumed that they were initiated by the Test Runner. If the conditions are met, the
     * [testRunner] is set as the client of the corresponding trace.
     */
    private fun reviseClients(traces: List<AggregatedNetworkTrace>) {
        for (trace in traces) {
            if (trace.parentId == null && trace.request.sourceIp == networkGateways[trace.networkId]?.ipAddress) {
                trace.client = testRunner
            }
        }
    }

    /**
     * Tries to find the Mock Server addressed in the given packet. Uses the information from
     * the host header of the request to find the corresponding Mock Server with the same name.
     */
    private fun findMockServer(packet: HttpRequestPacket): NetworkNodeSpecification? {
        val host = packet.headers.filterKeys { it.lowercase() == "host" }.firstNotNullOfOrNull { it.value }
        if (host.isNullOrEmpty()) {
            logger.warn("Host header is not set for request $packet.")
            return null
        }
        return mockServers[host.first()]
    }

    /**
     * Extension function to find all traces which were captured between the timestamps of the
     * given request and response packet.
     */
    private fun List<AggregatedNetworkTrace>.findTracesInBetween(
        request: AggregatedNetworkTrace.RequestPacket,
        response: AggregatedNetworkTrace.ResponsePacket,
    ): List<AggregatedNetworkTrace> {
        return this.filter { it.request.timestamp > request.timestamp && it.response.timestamp < response.timestamp }
    }

    /**
     * Extension function to find all traces for which the request's timestamp is between the
     * given start (inclusive) and end time (inclusive).
     */
    private fun List<AggregatedNetworkTrace>.findTracesBetween(start: Instant, end: Instant): List<AggregatedNetworkTrace> {
        return this.filter { it.request.timestamp in start..end }
    }

    /**
     * Initializes network trace collector for the network with the given ID to start capturing the
     * network's HTTP traffic.
     */
    private fun startNetworkMonitoring(networkId: String) {
        val monitor = NetworkTraceCollector(networkId)
        networkTraceCollectors[networkId] = monitor
        monitor.start()
    }

    /**
     * Registers the given container with its IP address in the given network to be able to associate packets from and
     * to this container with the corresponding container instance. If the container does not have an IP address assigned
     * in the given network, it is ignored.
     */
    private fun registerContainer(network: ContainerNetwork, container: Container, specification: ServiceSpecification) {
        if (network.ipAddress.isNullOrBlank()) {
            logger.warn(
                "No IP address set for container ${container.name} in network $network. " +
                    "Unable to associate HTTP packets from and to this container with the container instance."
            )
            return
        }
        val networkContainers = monitoredNetworks[network.networkId] ?: mutableMapOf()
        networkContainers[network.ipAddress] = NetworkNodeSpecification(
            nodeType = NetworkNodeSpecification.NodeType.CONTAINER,
            ipAddress = network.ipAddress,
            specification = specification,
        )
        monitoredNetworks[network.networkId] = networkContainers
        logger.info("Registered IP address ${network.ipAddress} of container '${container.name}' in network ${network.networkId}.")
    }

    /**
     * Initializes a network trace collector for the given network if it is not already being monitored.
     * Registers a network node specification for the network's gateway, if available.
     */
    private fun initializeNetworkTraceCollector(networkName: String, network: ContainerNetwork) {
        if (network.networkId !in networkTraceCollectors) {
            startNetworkMonitoring(network.networkId)
        }
        if (network.gateway != null && network.networkId !in networkGateways) {
            networkGateways[network.networkId] = NetworkNodeSpecification(
                nodeType = NetworkNodeSpecification.NodeType.NETWORK_GATEWAY,
                ipAddress = network.gateway,
                specification = ServiceSpecification(name = networkName),
            )
        }
    }
}
