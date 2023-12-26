package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.config.utils.MockServerDeserializer
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper.Companion.METADATA_MOCK_SERVER_NAME_KEY
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
    }

    /**
     * Registers all stubs defined for this mock server.
     * This leads to the mock server responding with the specified response whenever the request matches the specified stub.
     * @throws IllegalStateException if the mock server is not initialized.
     */
    fun registerStubs() {
        checkNotNull(wireMockServer) { "Mock server needs to be initialized" }
        for (stub in this.stubs) {
            stub.registerAt(name, wireMockServer!!)
        }
    }

    /**
     * Resets all stubs and registered requests for this mock server.
     * @throws IllegalStateException if the mock server is not initialized.
     */
    fun reset() {
        checkNotNull(wireMockServer) { "Mock server needs to be initialized" }
        val metadataMatcher = WireMock.matchingJsonPath("$.$METADATA_MOCK_SERVER_NAME_KEY", WireMock.equalTo(name))
        wireMockServer!!.removeStubsByMetadata(metadataMatcher)
        wireMockServer!!.removeServeEventsForStubsMatchingMetadata(metadataMatcher)
    }

    /**
     * Returns all requests that this mock server received sorted by their timestamp.
     * @throws IllegalStateException if the HTTP mock server is not initialized or not running.
     */
    val requestsReceived: List<HttpRequest>
        get() {
            val events = wireMockRequestsReceived.toMutableList()
            events.sortBy { it.request.loggedDate }
            return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
        }

    /**
     * Returns all requests as [ServeEvent] instances that this mock server received.
     * @throws IllegalStateException if the HTTP mock server is not initialized or not running.
     */
    private val wireMockRequestsReceived: List<ServeEvent>
        get() {
            check(wireMockServer != null && wireMockServer!!.isRunning) { "Received requests can only be retrieved when mock server is running" }
            return wireMockServer!!.allServeEvents.filter { it.request.host == hostname }
        }

}
