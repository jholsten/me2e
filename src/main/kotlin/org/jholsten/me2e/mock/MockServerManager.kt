package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.FatalStartupException
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.awaitility.core.ConditionTimeoutException
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.healthcheck.exception.ServiceNotHealthyException
import org.jholsten.me2e.mock.stubbing.MockServerStubNotMatchedRenderer
import org.jholsten.me2e.utils.logger

/**
 * Service that manages all [MockServer] instances.
 * Manages one HTTP mock server for all [MockServer] instances and enables a multi-domain mapping.
 * As only one single HTTP server needs to be started, performance is improved.
 */
class MockServerManager(
    /**
     * Mock servers mocking third-party services.
     */
    private val mockServers: Map<String, MockServer>,
) {
    private val logger = logger(this)

    /**
     * Mock server instance that handles incoming requests
     */
    private val wireMockServer: WireMockServer = WireMockServer(
        WireMockConfiguration()
            .port(80)
            .httpsPort(443)
    )
    // TODO: Methods to reset + register stubs

    /**
     * Initializes all mock server instances and starts mocked HTTP server.
     * Waits at most 5 seconds until the HTTP server is running.
     * @throws ServiceStartupException if server could not be started.
     * @throws ServiceNotHealthyException if server is not running within 5 seconds.
     */
    fun start() {
        initializeMockServers()
        startHTTPMockServer()
    }

    /**
     * Stops HTTP mock server. The server also stops automatically when all tests are finished.
     */
    fun stop() {
        this.wireMockServer.stop()
        logger.info("Stopped HTTP mock server")
    }

    /**
     * Returns whether the HTTP mock server is currently up and running.
     */
    val isRunning: Boolean
        get() = this.wireMockServer.isRunning

    /**
     * Returns the HTTP port of the HTTP mock server.
     * @throws IllegalStateException if the mock server is not running.
     */
    val httpPort: Int
        get() {
            check(isRunning) { "Mock server needs to be started to retrieve the HTTP port." }
            return wireMockServer.port()
        }

    /**
     * Returns the HTTPS port of the HTTP mock server.
     * @throws IllegalStateException if the mock server is not running.
     */
    val httpsPort: Int
        get() {
            check(isRunning) { "Mock server needs to be started to retrieve the HTTPS port." }
            return wireMockServer.httpsPort()
        }

    /**
     * Starts mocked HTTP server and waits at most 5 seconds until it's running.
     * @throws ServiceStartupException if server could not be started.
     * @throws ServiceNotHealthyException if server is not running within 5 seconds.
     */
    private fun startHTTPMockServer() {
        try {
            logger.info("Starting HTTP mock server...")
            this.wireMockServer.start()
            Awaitility.await().atMost(Durations.FIVE_SECONDS).until { wireMockServer.isRunning }
            logger.info("Started HTTP mock server on port ${wireMockServer.port()}.")
        } catch (e: FatalStartupException) {
            throw ServiceStartupException("Mock server could not be started: ${e.message}")
        } catch (e: ConditionTimeoutException) {
            throw ServiceNotHealthyException("Mock server was not running within 5 seconds.")
        }
    }

    /**
     * Initializes all mock servers by registered their stubs at the [WireMockServer] instance.
     */
    private fun initializeMockServers() {
        logger.info("Initializing ${mockServers.size} mock servers...")
        for ((mockServerName, mockServer) in mockServers) {
            mockServer.initialize(wireMockServer)
            logger.info("Initialized mock server $mockServerName")
        }
    }
}
