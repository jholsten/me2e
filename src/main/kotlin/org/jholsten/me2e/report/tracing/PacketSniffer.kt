package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.GenericContainer

/**
 * Service which sniffs all TCP packages in Docker networks.
 */
class PacketSniffer {
    private val logger = logger(this)

    /**
     * List of Docker network IDs for which the TCP traffic is recorded.
     */
    private val monitoredNetworks: MutableMap<String, MutableList<ContainerNetworkSpecification>> = mutableMapOf()

    private val networkMonitors: MutableMap<String, NetworkMonitor> = mutableMapOf()

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
            if (!networkMonitors.containsKey(network.networkId)) {
                startNetworkMonitoring(network.networkId)
            }
        }
    }

    fun collectPackets() {
        val packets: MutableList<Any> = mutableListOf()
        for ((networkId, monitor) in networkMonitors) {
            val networkPackets = monitor.collectPackets()
            packets.add(matchSourceAndDestination(networkId, networkPackets))
        }
    }

    private fun matchSourceAndDestination(networkId: String, packet: Any) {
        // TODO: Find service (i.e. Mock Server, Test Runner or Container) for Source + IP Addresses
        // TODO: Return packet with ServiceSpecifications
    }

    private fun startNetworkMonitoring(networkId: String) {
        val monitor = NetworkMonitor(networkId)
        networkMonitors[networkId] = monitor
        monitor.start()
    }

    private data class ContainerNetworkSpecification(
        val containerSpecification: ServiceSpecification,
        val ipAddress: String?,
    )
}
