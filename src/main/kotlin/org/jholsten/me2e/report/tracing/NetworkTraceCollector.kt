package org.jholsten.me2e.report.tracing

import com.fasterxml.jackson.core.type.TypeReference
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.report.tracing.model.HttpPacket
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.lang.Exception

/**
 * Service which collects all HTTP packets sent in the Docker network with ID [networkId].
 * Uses the [docker-traffic-capturer](https://gitlab.informatik.uni-bremen.de/jholsten/docker-traffic-capturer)
 * to sniff the packets which were sent in the network.
 * As there can be delays when receiving and processing the captured packets in the `docker-traffic-capturer`
 * and the packets are not available immediately after the test execution, they are only collected after all
 * tests have been executed. The assignment of the packets to the tests then takes place using the timestamps
 * of the captured packets.
 */
class NetworkTraceCollector(
    /**
     * ID of the Docker bridge network for which HTTP packets should be collected.
     */
    private val networkId: String,
) {
    private val logger = logger(this)

    /**
     * [docker-traffic-capturer](https://gitlab.informatik.uni-bremen.de/jholsten/docker-traffic-capturer) which collects
     * all HTTP packets sent in the network.
     */
    private val capturer = GenericContainer(DockerImageName.parse(DOCKER_TRAFFIC_CAPTURER_IMAGE))
        .withNetworkMode("host")
        .withEnv("NETWORK_ID", networkId)
        .waitingFor(Wait.forLogMessage(".*Capture started.*", 1))

    /**
     * Starts container for capturing HTTP packets in the network.
     */
    fun start() {
        capturer.start()
        logger.info("Started network traffic capturer for network ID $networkId")
    }

    /**
     * Collects HTTP packets sent in this network from the [capturer].
     * @return All packets captured in this network.
     */
    fun collect(): List<HttpPacket> {
        logger.info("Collecting packets...")
        try {
            val result = capturer.execInContainer("sh", "collect.sh")
            if (result.exitCode != 0) {
                logger.warn(
                    "Traffic capturer of network $networkId responded with unsuccessful exit code: ${result.exitCode}. " +
                        "Output: ${result.stderr}. Unable to collect packets."
                )
                return listOf()
            }

            return DESERIALIZER.readValue(result.stdout, object : TypeReference<List<HttpPacket>>() {})
        } catch (e: Exception) {
            logger.warn("Unable to collect packets from network $networkId.")
            e.printStackTrace()
            return listOf()
        }
    }

    companion object {
        private const val DOCKER_TRAFFIC_CAPTURER_IMAGE = "gitlab.informatik.uni-bremen.de:5005/jholsten/docker-traffic-capturer:latest"
        private val DESERIALIZER = DeserializerFactory.getObjectMapper()
    }
}
