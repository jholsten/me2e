package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.FatalStartupException
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.awaitility.core.ConditionTimeoutException
import org.jholsten.me2e.config.model.MockServerConfig
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.health.exception.HealthTimeoutException
import org.jholsten.me2e.mock.stubbing.MockServerStubNotMatchedRenderer
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.utils.PortUtils
import org.jholsten.me2e.utils.logger

/**
 * Service that manages all [MockServer] instances.
 * Manages one HTTP Mock Server for all [MockServer] instances and enables a multi-domain mapping.
 * As only one single HTTP server needs to be started, performance is improved.
 */
class MockServerManager internal constructor(
    /**
     * Mock Servers mocking third-party services.
     */
    val mockServers: Map<String, MockServer>,

    /**
     * Configuration for all [org.jholsten.me2e.mock.MockServer] instances.
     */
    mockServerConfig: MockServerConfig,
) {
    companion object {
        /**
         * Port where the Mock Server's HTTP API is reachable.
         * Uses the default HTTP port to be able to use the Mock Servers hostname without the need to specify a port.
         */
        private const val HTTP_PORT = 80

        /**
         * Port where the Mock Server's HTTPS API is reachable.
         * Uses the default HTTPS port to be able to use the Mock Servers hostname without the need to specify a port.
         */
        private const val HTTPS_PORT = 443
    }

    private val logger = logger<MockServerManager>()

    /**
     * Mock Server instance that handles incoming requests.
     * Is shared between all [MockServer] instances.
     */
    private val wireMockServer: WireMockServer

    init {
        val configuration = WireMockConfiguration()
            .port(HTTP_PORT)
            .httpsPort(HTTPS_PORT)
            .keystoreType(mockServerConfig.keystoreType)
            .trustStoreType(mockServerConfig.truststoreType)
            .needClientAuth(mockServerConfig.needsClientAuth)
            .notMatchedRenderer(MockServerStubNotMatchedRenderer())
        mockServerConfig.keystorePath?.let { configuration.keystorePath(it) }
        mockServerConfig.keystorePassword?.let { configuration.keystorePassword(it) }
        mockServerConfig.keyManagerPassword?.let { configuration.keyManagerPassword(it) }
        mockServerConfig.keyManagerPassword?.let { configuration.keyManagerPassword(it) }
        mockServerConfig.truststorePath?.let { configuration.trustStorePath(it) }
        mockServerConfig.truststorePassword?.let { configuration.trustStorePassword(it) }

        wireMockServer = WireMockServer(configuration)
        initializeMockServers()
    }

    /**
     * Returns all requests that any Mock Server received sorted by their timestamp.
     * @throws IllegalStateException if the Mock Server is not running.
     */
    val requestsReceived: List<HttpRequest>
        get() {
            check(wireMockServer.isRunning) { "Received requests can only be retrieved when Mock Server is running." }
            val events = wireMockServer.allServeEvents.toMutableList()
            events.sortBy { it.request.loggedDate }
            return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
        }

    /**
     * Returns whether the HTTP Mock Server is currently up and running.
     */
    val isRunning: Boolean
        get() = this.wireMockServer.isRunning

    /**
     * Returns the HTTP port of the Mock Server.
     */
    val httpPort: Int
        get() = HTTP_PORT

    /**
     * Returns the HTTPS port of the Mock Server.
     */
    val httpsPort: Int
        get() = HTTPS_PORT

    /**
     * Initializes all Mock Server instances and starts mocked HTTP server.
     * Waits at most 5 seconds until the HTTP server is running.
     * @throws ServiceStartupException if server could not be started.
     * @throws HealthTimeoutException if server is not running within 5 seconds.
     * @throws IllegalStateException if Mock Server is already running.
     */
    fun start() {
        check(!isRunning) { "Mock Server is already running" }
        if (mockServers.isNotEmpty()) {
            assertPortsAreAvailable()
            registerAllStubs()
            startHTTPMockServer()
        }
    }

    /**
     * Stops HTTP Mock Server. The server also stops automatically when all tests are finished.
     * @throws IllegalStateException if Mock Server is currently not running
     */
    fun stop() {
        check(isRunning) { "Mock Server can only be stopped if it is currently running" }
        this.wireMockServer.stop()
        logger.info("Stopped HTTP Mock Server.")
    }

    /**
     * Resets all captured requests for all Mock Server instances.
     */
    fun resetAll() {
        wireMockServer.resetRequests()
    }

    /**
     * Starts mocked HTTP server and waits at most 5 seconds until it's running.
     * @throws ServiceStartupException if server could not be started.
     * @throws HealthTimeoutException if server is not running within 5 seconds.
     */
    private fun startHTTPMockServer() {
        try {
            logger.info("Starting HTTP Mock Server...")
            this.wireMockServer.start()
            Awaitility.await().atMost(Durations.FIVE_SECONDS).until { wireMockServer.isRunning }
            logger.info("Started HTTP Mock Server on port ${wireMockServer.port()}.")
        } catch (e: FatalStartupException) {
            throw ServiceStartupException("Mock Server could not be started: ${e.message}")
        } catch (e: ConditionTimeoutException) {
            throw HealthTimeoutException("Mock Server was not running within 5 seconds.")
        }
    }

    /**
     * Initializes all Mock Servers by setting the reference to this [wireMockServer] instance.
     */
    private fun initializeMockServers() {
        logger.info("Initializing ${mockServers.size} Mock Servers...")
        for ((mockServerName, mockServer) in mockServers) {
            mockServer.initialize(wireMockServer)
            logger.info("Initialized Mock Server $mockServerName")
        }
    }

    /**
     * Registers all stubs defined for all Mock Servers.
     * This leads to the Mock Servers responding with the specified response whenever the request matches the corresponding stub.
     * @throws IllegalStateException if the Mock Server is not initialized.
     */
    private fun registerAllStubs() {
        for (mockServer in mockServers.values) {
            mockServer.registerStubs()
        }
    }

    private fun assertPortsAreAvailable() {
        for (port in listOf(HTTP_PORT, HTTPS_PORT)) {
            if (!PortUtils.isPortAvailable(port)) {
                throw ServiceStartupException("Port $port is already in use")
            }
        }
    }
}
