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
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.utils.isPortAvailable
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
    companion object {
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
    }

    private val logger = logger(this)

    /**
     * Mock server instance that handles incoming requests
     */
    private val wireMockServer: WireMockServer = WireMockServer(
        WireMockConfiguration()
            .port(HTTP_PORT)
            .httpsPort(HTTPS_PORT)
            .notMatchedRenderer(MockServerStubNotMatchedRenderer())
    )

    /**
     * Initializes all mock server instances and starts mocked HTTP server.
     * Waits at most 5 seconds until the HTTP server is running.
     * @throws ServiceStartupException if server could not be started.
     * @throws ServiceNotHealthyException if server is not running within 5 seconds.
     * @throws IllegalStateException if mock server is already running
     */
    fun start() {
        check(!isRunning) { "Mock server is already running" }
        assertPortsAreAvailable()
        initializeMockServers()
        registerAllStubs()
        startHTTPMockServer()
    }

    /**
     * Stops HTTP mock server. The server also stops automatically when all tests are finished.
     * @throws IllegalStateException if mock server is currently not running
     */
    fun stop() {
        check(isRunning) { "Mock server can only be stopped if it is currently running" }
        this.wireMockServer.stop()
        logger.info("Stopped HTTP mock server")
    }

    /**
     * Registers all stubs defined for all mock servers.
     * This leads to the mock servers responding with the specified response whenever the request matches the specified stub.
     * @throws IllegalStateException if the mock server is not initialized.
     */
    fun registerAllStubs() {
        for (mockServer in mockServers.values) {
            mockServer.registerStubs()
        }
    }

    /**
     * Resets all stubs and registered requests for all mock servers.
     * @throws IllegalStateException if the mock server is not initialized.
     */
    fun resetAll() {
        for (mockServer in mockServers.values) {
            mockServer.reset()
        }
        wireMockServer.resetAll()
    }

    /**
     * Returns all requests that any mock server received sorted by their timestamp.
     * @throws IllegalStateException if the HTTP mock server is not running.
     */
    val requestsReceived: List<HttpRequest>
        get() {
            check(wireMockServer.isRunning) { "Received requests can only be retrieved when mock server is running" }
            val events = wireMockServer.allServeEvents.toMutableList()
            events.sortBy { it.request.loggedDate }
            return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
        }

    /**
     * Returns whether the HTTP mock server is currently up and running.
     */
    val isRunning: Boolean
        get() = this.wireMockServer.isRunning

    /**
     * Returns the HTTP port of the HTTP mock server.
     */
    val httpPort: Int
        get() = HTTP_PORT

    /**
     * Returns the HTTPS port of the HTTP mock server.
     */
    val httpsPort: Int
        get() = HTTPS_PORT

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

    private fun assertPortsAreAvailable() {
        for (port in listOf(HTTP_PORT, HTTPS_PORT)) {
            if (!isPortAvailable(port)) {
                throw ServiceStartupException("Port $port is already in use")
            }
        }
    }
}
