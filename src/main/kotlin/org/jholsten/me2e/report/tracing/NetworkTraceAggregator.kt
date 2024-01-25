package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.tracing.model.HttpPacket
import org.jholsten.me2e.utils.logger

/**
 * Service which aggregates all HTTP packets sent in the Docker networks,
 * which the containers are part of. Enables to trace requests and responses.
 * Matches IP addresses to the associated services.
 */
class NetworkTraceAggregator {
    private val logger = logger(this)

    /**
     * List of Docker network IDs for which the TCP traffic is recorded.
     */
    private val monitoredNetworks: MutableMap<String, MutableList<ContainerNetworkSpecification>> = mutableMapOf()

    private val networkTraceCollectors: MutableMap<String, NetworkTraceCollector> = mutableMapOf()

    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        val networks = container.networks.values
        for (network in networks) {
            if (network.ipAddress == null) {
                logger.warn(
                    "No IP address set for container ${container.name} in network $network. " +
                        "Unable to associate TCP packets from and to this container with the container instance."
                )
            }
            val networkContainers = monitoredNetworks[network.networkId] ?: mutableListOf()
            networkContainers.add(ContainerNetworkSpecification(specification, network.ipAddress))
            if (!networkTraceCollectors.containsKey(network.networkId)) {
                startNetworkMonitoring(network.networkId)
            }
        }
    }

    fun collectPackets(testId: String) {
        val packets: MutableList<HttpPacket> = mutableListOf()
        for ((networkId, monitor) in networkTraceCollectors) {
            val networkPackets = monitor.collect()
            //packets.add(matchSourceAndDestination(networkId, networkPackets))
            packets.addAll(networkPackets)
        }
        packets.sortBy { it.timestamp }
        println(packets)
    }

//    private fun matchSourceAndDestination(networkId: String, packet: Any): HttpPacket {
//        // TODO: Find service (i.e. Mock Server, Test Runner or Container) for Source + IP Addresses
//        // TODO: Return packet with ServiceSpecifications
//        return HttpPacket()
//    }

    private fun startNetworkMonitoring(networkId: String) {
        val monitor = NetworkTraceCollector(networkId)
        networkTraceCollectors[networkId] = monitor
        //monitor.start()
    }

    private data class ContainerNetworkSpecification(
        val containerSpecification: ServiceSpecification,
        val ipAddress: String?,
    )
}
