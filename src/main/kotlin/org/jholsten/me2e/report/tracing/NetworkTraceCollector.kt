package org.jholsten.me2e.report.tracing

import org.jholsten.me2e.report.tracing.model.HttpPacket
import org.jholsten.me2e.request.client.OkHttpClient
import org.jholsten.me2e.request.model.RelativeUrl
import org.jholsten.me2e.request.model.Url
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

/**
 * Service which collects all HTTP packets sent in the Docker network with ID [networkId].
 * Uses the [docker-traffic-capturer](https://gitlab.informatik.uni-bremen.de/jholsten/docker-traffic-capturer)
 * to sniff the packets which were sent in the network.
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
        //.withExposedPorts(8000)
        .withEnv("NETWORK_ID", networkId)
    //.waitingFor(Wait.forHttp("/health"))

    /**
     * HTTP client for executing requests towards the [capturer].
     */
    private lateinit var httpClient: OkHttpClient

    fun start() {
        capturer.start()
        //val port = capturer.firstMappedPort
        //httpClient = OkHttpClient.Builder().withBaseUrl(Url("http://${capturer.host}:${port}")).build()
    }

    /**
     * Collects HTTP packets sent in this network from the [capturer].
     * @return Packages captured since the last time this method was called.
     */
    @JvmSynthetic
    internal fun collect(): List<HttpPacket> {
//        //val body = httpClient.get(RelativeUrl("/collect")).body
//        if (body == null) {
//            logger.error("Docker network capturer did not provide a body.")
//            return listOf()
//        }
//
//        return body.asObject<List<HttpPacket>>()!!
        capturer.execInContainer("curl", "http://localhost:8000/collect")
        return listOf()
    }

    companion object {
        private const val DOCKER_TRAFFIC_CAPTURER_IMAGE = "gitlab.informatik.uni-bremen.de:5005/jholsten/docker-traffic-capturer:latest"
    }
}
