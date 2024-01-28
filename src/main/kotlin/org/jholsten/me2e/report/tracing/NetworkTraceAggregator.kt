package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.Me2eTestConfigStorage
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.network.ContainerNetwork
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.report.tracing.model.HttpPacket
import org.jholsten.me2e.report.tracing.model.HttpRequestPacket
import org.jholsten.me2e.report.tracing.model.HttpResponsePacket
import org.jholsten.me2e.report.tracing.model.NetworkNodeSpecification
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
    private val monitoredNetworks: MutableMap<String, MutableMap<String, ServiceSpecification>> = mutableMapOf()

    private val networkGateways: MutableMap<String, String> = mutableMapOf()

    /**
     * Collectors which are
     */
    private val networkTraceCollectors: MutableMap<String, NetworkTraceCollector> = mutableMapOf()

    private val mockServers: Map<String, ServiceSpecification> = if (Me2eTestConfigStorage.config != null) {
        Me2eTestConfigStorage.config!!.environment.mockServers.values
            .associate {
                it.hostname to ServiceSpecification(name = "Mock Server ${it.name}")
            }
    } else {
        mapOf()
    }

    /**
     * IP address of the host which is running the tests.
     */
    private val hostIpAddress: String by lazy {
        System.getenv("RUNNER_IP") ?: getHostDockerInternalIp()
    }

    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        val networks = container.networks.values
        for (network in networks) {
            initializeNetworkTraceCollector(network, container, specification)
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
        aggregatePackets(packets)
        println(packets)
    }

    private fun aggregatePackets(packets: MutableList<HttpPacket>) {
        packets.sortBy { it.timestamp }
        val responsePackets = packets.sortedByDescending { it.timestamp }.filterIsInstance<HttpResponsePacket>()
        val aggregatedPackets: MutableList<IntermediateAggregatedPacket> = mutableListOf()
        val requestResponses: MutableMap<IntermediateAggregatedPacket, IntermediateAggregatedPacket> = mutableMapOf()
        for (responsePacket in responsePackets) {
            val requestPacket = findCorrespondingRequest(responsePacket, packets)
            if (requestPacket == null) {
                logger.warn("Unable to find corresponding request for response $responsePacket.")
                continue
            }
            val (client, server) = matchSourceAndDestination(requestPacket)
            val aggregatedRequest = IntermediateAggregatedPacket(source = client, destination = server, packet = requestPacket)
            val aggregatedResponse = IntermediateAggregatedPacket(source = server, destination = client, packet = responsePacket)
            aggregatedPackets.add(aggregatedRequest)
            aggregatedPackets.add(aggregatedResponse)
            requestResponses[aggregatedRequest] = aggregatedResponse
        }
        aggregatedPackets.sortBy { it.packet.timestamp }
        for ((request, response) in requestResponses) {
            val requestsInBetween = aggregatedPackets.subList(aggregatedPackets.indexOf(request) + 1, aggregatedPackets.indexOf(response))
                .filter { it.packet is HttpRequestPacket }
            for (intermediateRequest in requestsInBetween) {
                if (intermediateRequest.packet.sourceIp == request.packet.destinationIp) {
                    intermediateRequest.parentId = request.id
                }
            }
            if (request.parentId == null && request.packet.sourceIp == networkGateways[request.packet.networkId]) {
                request.source = ReportDataAggregator.testRunner
            }
            response.destination = request.source
            response.parentId = request.parentId
        }
//        for (i in 0 until aggregatedPackets.size) {
//            val aggregatedPacket = aggregatedPackets[i]
//            if (aggregatedPacket.packet.response != null) {
//                val aggregatedRequest = requestResponses[aggregatedPacket]!!
//                aggregatedPacket.destination = aggregatedRequest.source
//                aggregatedPacket.parentId = aggregatedRequest.parentId
//                continue
//            } else if (aggregatedPacket.packet.request != null) {
//
//            }
//        }
        println("")
    }

    private fun findCorrespondingRequest(responsePacket: HttpResponsePacket, packets: List<HttpPacket>): HttpPacket? {
        val networkPackets = packets.filter { it.networkId == responsePacket.networkId }
        if (responsePacket.requestIn != null) {
            return networkPackets.find { it.number == responsePacket.requestIn }
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
                val networkGateway = networkGateways[responsePacket.networkId]
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
    private fun matchSourceAndDestination(packet: HttpPacket): Pair<ServiceSpecification?, ServiceSpecification?> {
        val source = matchIpAndPort(packet.networkId, packet.sourceIp, packet.sourcePort, packet)
        val destination = matchIpAndPort(packet.networkId, packet.destinationIp, packet.destinationPort, packet)
        return source to destination
    }

    /**
     * Tries to find the service in the network which is associated with the given IP address.
     * If the service is not found in the list of registered containers, it may be the test runner
     * or one of the mock servers which sent/received the associated packet.
     */
    private fun matchIpAndPort(networkId: String, ipAddress: String, port: Int, packet: HttpPacket): ServiceSpecification? {
        val networkContainers = monitoredNetworks[networkId] ?: return null
        if (ipAddress in networkContainers) {
            return networkContainers[ipAddress]
        }

        if (ipAddress == networkGateways[networkId]) {
            return ServiceSpecification(name = "Network Gateway")
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
            return ReportDataAggregator.testRunner
        }
        // TODO
        return null
    }

    private fun startNetworkMonitoring(networkId: String) {
        val monitor = NetworkTraceCollector(networkId)
        networkTraceCollectors[networkId] = monitor
        monitor.start()
    }

    private fun initializeNetworkTraceCollector(network: ContainerNetwork, container: Container, specification: ServiceSpecification) {
        if (network.ipAddress == null) {
            logger.warn(
                "No IP address set for container ${container.name} in network $network. " +
                    "Unable to associate HTTP packets from and to this container with the container instance."
            )
            return
        }
        val networkContainers = monitoredNetworks[network.networkId] ?: mutableMapOf()
        networkContainers[network.ipAddress] = specification
        monitoredNetworks[network.networkId] = networkContainers
        if (!networkTraceCollectors.containsKey(network.networkId)) {
            if (network.gateway != null) {
                networkGateways[network.networkId] = network.gateway
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

    private data class IntermediateAggregatedPacket(
        val id: String = UUID.randomUUID().toString(),
        var source: ServiceSpecification?,
        var destination: ServiceSpecification?,
        val packet: HttpPacket,
        var parentId: String? = null,
        var assigned: Boolean = false,
    )
}
