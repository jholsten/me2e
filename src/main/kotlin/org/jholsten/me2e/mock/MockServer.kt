package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.FatalStartupException
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import okhttp3.internal.toImmutableList
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.awaitility.core.ConditionTimeoutException
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.healthcheck.exception.ServiceNotHealthyException
import org.jholsten.me2e.mock.parser.YamlMockServerStubParser
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest


/**
 * Model representing a third party service to be mocked.
 */
class MockServer(
    /**
     * Unique name of this mock server.
     */
    val name: String,

    /**
     * Port where this mocked web server is exposed
     */
    val port: Int,

    /**
     * List of paths to stub definitions. The files need to be located in `resources` folder.
     */
    stubs: List<String> = listOf(),
) {
    /**
     * Definition of stubs for this mock server
     */
    val stubs: List<MockServerStub>

    /**
     * Mock server instance that handles incoming requests
     */
    private val wireMockServer: WireMockServer = WireMockServer(WireMockConfiguration().port(this.port))

    init {
        this.stubs = readStubs(stubs)
        for (stub in this.stubs) {
            stub.register(wireMockServer)
        }
    }

    /**
     * Starts this mocked HTTP server and waits at most 5 seconds until it's running.
     * @throws ServiceStartupException if server could not be started.
     * @throws ServiceNotHealthyException if server is not running within 5 seconds.
     */
    fun start() {
        try {
            this.wireMockServer.start()
            await().atMost(Durations.FIVE_SECONDS).until { wireMockServer.isRunning }
        } catch (e: FatalStartupException) {
            throw ServiceStartupException("Mock server $name could not be started: ${e.message}")
        } catch (e: ConditionTimeoutException) {
            throw ServiceNotHealthyException("Mock server $name was not running within 5 seconds.")
        }
    }

    /**
     * Stops this mocked HTTP server.
     */
    fun stop() {
        this.wireMockServer.stop()
    }

    /**
     * Returns whether this mock server is currently up and running.
     */
    fun isRunning(): Boolean {
        return this.wireMockServer.isRunning
    }

    /**
     * Returns all requests that this mock server received.
     */
    fun getReceivedRequests(): List<HttpRequest> {
        val events = wireMockServer.allServeEvents
        events.sortBy { it.request.loggedDate }
        return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
    }

    private fun readStubs(stubFiles: List<String>): List<MockServerStub> {
        val parser = YamlMockServerStubParser()
        return stubFiles.map { parser.parseFile(it) }.toImmutableList()
    }
}
