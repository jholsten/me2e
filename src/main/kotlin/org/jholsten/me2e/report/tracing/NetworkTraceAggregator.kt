package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.network.ContainerNetwork
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.tracing.model.AggregatedHttpPacket
import org.jholsten.me2e.report.tracing.model.HttpPacket
import org.jholsten.me2e.utils.logger

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

    /**
     * Collectors which are
     */
    private val networkTraceCollectors: MutableMap<String, NetworkTraceCollector> = mutableMapOf()

    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        val networks = container.networks.values
        for (network in networks) {
            initializeNetworkTraceCollector(network, container, specification)
        }
    }

    fun collectPackets(testId: String) {
        val packets: MutableList<HttpPacket> = mutableListOf()
        for ((networkId, monitor) in networkTraceCollectors) {
            val networkPackets = monitor.collect()
            packets.addAll(aggregateNetworkPackets(networkId, networkPackets))
        }
        packets.sortBy { it.timestamp }
        println(packets)
    }

    private fun aggregateNetworkPackets(networkId: String, packets: List<HttpPacket>): List<AggregatedHttpPacket> {
        val result: MutableList<AggregatedHttpPacket> = mutableListOf()
        for (packet in packets) {
            val (source, destination) = matchSourceAndDestination(networkId, packet)
            result.add(
                AggregatedHttpPacket(
                    timestamp = packet.timestamp,
                    source = source,
                    sourceIp = packet.sourceIp,
                    sourcePort = packet.sourcePort,
                    destination = destination,
                    destinationIp = packet.destinationIp,
                    destinationPort = packet.destinationPort,
                    request = packet.request,
                    response = packet.response,
                )
            )
        }
        return result
    }

    /**
     * Matches the source and destination IP addresses to the services in the network.
     * If no such service can be found, `null` is returned.
     * @return Pair of source service and destination service.
     */
    private fun matchSourceAndDestination(networkId: String, packet: HttpPacket): Pair<ServiceSpecification?, ServiceSpecification?> {
        val source = matchIpAndPort(networkId, packet.sourceIp, packet.sourcePort)
        val destination = matchIpAndPort(networkId, packet.destinationIp, packet.destinationPort)
        return source to destination
    }

    /**
     * Tries to find the service in the network which is associated with the given IP address.
     * If the service is not found in the list of registered containers, it may be the test runner
     * or one of the mock servers which sent/received the associated packet.
     */
    private fun matchIpAndPort(networkId: String, ipAddress: String, port: Int): ServiceSpecification? {
        val networkContainers = monitoredNetworks[networkId] ?: return null
        if (ipAddress in networkContainers) {
            return networkContainers[ipAddress]
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
            startNetworkMonitoring(network.networkId)
        }
    }
}
