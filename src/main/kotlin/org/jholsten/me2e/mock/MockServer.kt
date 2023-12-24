package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.tomakehurst.wiremock.WireMockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.config.utils.MockServerDeserializer
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest


/**
 * Model representing a third party service to be mocked.
 */
@JsonDeserialize(using = MockServerDeserializer::class)
class MockServer(
    /**
     * Unique name of this mock server.
     */
    val name: String,

    /**
     * Hostname of the service to be mocked
     */
    val hostname: String,

    /**
     * Definition of stubs for this mock server
     */
    val stubs: List<MockServerStub> = listOf(),
) {

    /**
     * Mock server instance that handles incoming requests
     */
    private var wireMockServer: WireMockServer? = null

    internal fun initialize(wireMockServer: WireMockServer) {
        this.wireMockServer = wireMockServer
        for (stub in this.stubs) {
            stub.registerAt(wireMockServer)
        }
    }

    /**
     * Returns all requests that this mock server received sorted by their timestamp.
     * @throws IllegalStateException if the HTTP mock server is not initialized or not running.
     */
    val requestsReceived: List<HttpRequest>
        get() {
            check(wireMockServer != null && wireMockServer!!.isRunning) { "Received requests can only be retrieved when mock server is running" }
            val events = wireMockServer!!.allServeEvents.filter { it.request.host == hostname }.toMutableList()
            events.sortBy { it.request.loggedDate }
            return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
        }
}
