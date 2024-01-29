package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.Me2eTestConfigStorage
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.network.ContainerNetwork
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.report.tracing.model.*
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.ToStringConsumer
import java.util.UUID

/**
 * Service which aggregates all HTTP packets sent in the Docker networks,
 * which the containers are part of. Enables to trace requests and responses.
 * Matches IP addresses to the associated services.
 * TODO: Error handling
 */
class NetworkTraceAggregator {
    private val logger = logger(this)

    /**
     * Docker network IDs for which the HTTP traffic is recorded as a map of
     * network IDs and (ip, service).
     */
    private val monitoredNetworks: MutableMap<String, MutableMap<String, NetworkNodeSpecification>> = mutableMapOf()

    private val networkGateways: MutableMap<String, NetworkNodeSpecification> = mutableMapOf()

    /**
     * Collectors which are
     */
    private val networkTraceCollectors: MutableMap<String, NetworkTraceCollector> = mutableMapOf()

    private val mockServers: Map<String, NetworkNodeSpecification> = if (Me2eTestConfigStorage.config != null) {
        Me2eTestConfigStorage.config!!.environment.mockServers.values
            .associate {
                it.hostname to NetworkNodeSpecification(
                    nodeType = NetworkNodeSpecification.NodeType.MOCK_SERVER,
                    ipAddress = hostIpAddress,
                    specification = ServiceSpecification(name = it.name),
                )
            }
    } else {
        mapOf()
    }

    /**
     * IP address of the host which is running the tests.
     */
    private val hostIpAddress: String by lazy {
        val ipAddress = System.getenv("RUNNER_IP") ?: getHostDockerInternalIp()
        logger.info("Host IP address is $ipAddress.")
        ipAddress
    }

    private val testRunner: NetworkNodeSpecification = NetworkNodeSpecification(
        nodeType = NetworkNodeSpecification.NodeType.SERVICE,
        ipAddress = hostIpAddress,
        specification = ReportDataAggregator.testRunner,
    )

    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        for ((networkName, network) in container.networks) {
            initializeNetworkTraceCollector(networkName, network, container, specification)
        }
    }

    /**
     * Collects and aggregates all captured packets in all networks.
     * Associates packets with the corresponding tests by their timestamps.
     */
    fun collectPackets(executedTests: List<TestResult>) {
        // Since network capturing may be delayed by a couple of milliseconds, we wait a little bit
        Thread.sleep(1000)
        val packets: MutableList<HttpPacket> = mutableListOf()
        for (monitor in networkTraceCollectors.values) {
            packets.addAll(monitor.collect())
        }
        val streams = aggregatePackets(packets)
        println(packets)
    }

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
     * Aggregates the given packets to instances of [AggregatedNetworkTrace].
     * Matches IP addresses to network nodes, responses to requests and nested requests and responses to streams.
     */
    private fun aggregatePackets(packets: MutableList<HttpPacket>): List<Pair<UUID, List<AggregatedNetworkTrace>>> {
        val aggregatedTraces = aggregateTraces(packets)
        val streams = associateStreams(aggregatedTraces)
        reviseClients(streams.flatMap { it.second })
        // TODO: Match to tests
        return streams
    }

    private fun associateStreams(aggregatedTraces: MutableList<AggregatedNetworkTrace>): List<Pair<UUID, List<AggregatedNetworkTrace>>> {
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
        return aggregatedTraces.groupBy { it.streamId }.map { it.key to it.value }.sortedBy { it.second.first().request.timestamp }
    }

    private fun reviseClients(traces: List<AggregatedNetworkTrace>) {
        for (trace in traces) {
            if (trace.parentId == null && trace.request.sourceIp == networkGateways[trace.networkId]?.ipAddress) {
                trace.client = testRunner
            }
        }
    }

    private fun List<AggregatedNetworkTrace>.findTracesInBetween(
        request: AggregatedNetworkTrace.RequestPacket,
        response: AggregatedNetworkTrace.ResponsePacket,
    ): List<AggregatedNetworkTrace> {
        return this.filter { it.request.timestamp > request.timestamp && it.response.timestamp < response.timestamp }
    }

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
     * Matches the source and destination IP addresses to the services in the network.
     * If no such service can be found, `null` is returned.
     * @return Pair of source service and destination service.
     */
    private fun matchSourceAndDestination(packet: HttpPacket): Pair<NetworkNodeSpecification?, NetworkNodeSpecification?> {
        val source = matchIpAndPort(packet.networkId, packet.sourceIp, packet.sourcePort, packet)
        val destination = matchIpAndPort(packet.networkId, packet.destinationIp, packet.destinationPort, packet)
        return source to destination
    }

    /**
     * Tries to find the service in the network which is associated with the given IP address.
     * If the service is not found in the list of registered containers, it may be the test runner
     * or one of the mock servers which sent/received the associated packet.
     */
    private fun matchIpAndPort(networkId: String, ipAddress: String, port: Int, packet: HttpPacket): NetworkNodeSpecification? {
        val networkContainers = monitoredNetworks[networkId] ?: return null
        if (ipAddress in networkContainers) {
            return networkContainers[ipAddress]
        }

        if (ipAddress == networkGateways[networkId]?.ipAddress) {
            return networkGateways[networkId]
        }

        if (ipAddress == hostIpAddress) {
            if (port == 80 && mockServers.isNotEmpty()) {
                if (packet is HttpRequestPacket) {
                    val host = packet.headers.filterKeys { it.lowercase() == "host" }.firstNotNullOfOrNull { it.value }
                    if (host == null) {
                        logger.warn("Host header is not set for request $packet.")
                        return null
                    }
                    return mockServers[host]
                }
            }
            return testRunner
        }
        logger.warn("Unable to find network node for IP address $ipAddress in network $networkId.")
        return null
    }

    private fun startNetworkMonitoring(networkId: String) {
        val monitor = NetworkTraceCollector(networkId)
        networkTraceCollectors[networkId] = monitor
        monitor.start()
    }

    private fun initializeNetworkTraceCollector(
        networkName: String,
        network: ContainerNetwork,
        container: Container,
        specification: ServiceSpecification
    ) {
        if (network.ipAddress == null) {
            logger.warn(
                "No IP address set for container ${container.name} in network $network. " +
                    "Unable to associate HTTP packets from and to this container with the container instance."
            )
            return
        }
        val networkContainers = monitoredNetworks[network.networkId] ?: mutableMapOf()
        networkContainers[network.ipAddress] = NetworkNodeSpecification(
            nodeType = NetworkNodeSpecification.NodeType.SERVICE,
            ipAddress = network.ipAddress,
            specification = specification,
        )
        monitoredNetworks[network.networkId] = networkContainers
        if (!networkTraceCollectors.containsKey(network.networkId)) {
            if (network.gateway != null) {
                networkGateways[network.networkId] = NetworkNodeSpecification(
                    nodeType = NetworkNodeSpecification.NodeType.NETWORK_GATEWAY,
                    ipAddress = network.gateway,
                    specification = ServiceSpecification(name = networkName),
                )
            }
            startNetworkMonitoring(network.networkId)
        }
    }

    private fun getHostDockerInternalIp(): String {
        val toStringConsumer = ToStringConsumer()
        val tempContainer = GenericContainer("alpine")
            .withCommand("sh", "-c", "getent hosts host.docker.internal | awk '{ print $1 }'")
            .withLogConsumer(toStringConsumer)
        tempContainer.start()

        return toStringConsumer.toUtf8String().replace("\n", "")
    }
}
