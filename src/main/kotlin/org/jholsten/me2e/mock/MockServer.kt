package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.config.utils.MockServerDeserializer
import org.jholsten.me2e.mock.exception.VerificationException
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper.Companion.METADATA_MOCK_SERVER_NAME_KEY
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.verification.MockServerVerification
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest


/**
 * Model representing a third party service to be mocked.
 */
@JsonDeserialize(using = MockServerDeserializer::class)
class MockServer(
    /**
     * Unique name of this Mock Server.
     */
    val name: String,

    /**
     * Hostname of the service to be mocked
     */
    val hostname: String,

    /**
     * Definition of stubs for this Mock Server
     */
    val stubs: List<MockServerStub> = listOf(),
) {

    /**
     * Returns whether the Mock Server is currently up and running.
     */
    val isRunning: Boolean
        get() = wireMockServer?.isRunning == true

    /**
     * Mock Server instance that handles incoming requests
     */
    private var wireMockServer: WireMockServer? = null

    internal fun initialize(wireMockServer: WireMockServer) {
        this.wireMockServer = wireMockServer
    }

    /**
     * Registers all stubs defined for this Mock Server.
     * This leads to the Mock Server responding with the specified response whenever the request matches the specified stub.
     * @throws IllegalStateException if the Mock Server is not initialized.
     */
    fun registerStubs() {
        checkNotNull(wireMockServer) { "Mock Server needs to be initialized" }
        for (stub in this.stubs) {
            stub.registerAt(name, wireMockServer!!)
        }
    }

    /**
     * Resets all stubs and registered requests for this Mock Server.
     * @throws IllegalStateException if the Mock Server is not initialized.
     */
    fun reset() {
        checkNotNull(wireMockServer) { "Mock Server needs to be initialized" }
        val metadataMatcher = WireMock.matchingJsonPath("$.$METADATA_MOCK_SERVER_NAME_KEY", WireMock.equalTo(name))
        wireMockServer!!.removeStubsByMetadata(metadataMatcher)
        wireMockServer!!.removeServeEventsForStubsMatchingMetadata(metadataMatcher)
    }

    /**
     * Returns all requests that this Mock Server received sorted by their timestamp.
     * @throws IllegalStateException if the HTTP Mock Server is not initialized or not running.
     */
    val requestsReceived: List<HttpRequest>
        get() {
            val events = wireMockRequestsReceived.toMutableList()
            events.sortBy { it.request.loggedDate }
            return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
        }

    /**
     * Returns all requests as [ServeEvent] instances that this Mock Server received.
     * @throws IllegalStateException if the HTTP Mock Server is not initialized or not running.
     */
    private val wireMockRequestsReceived: List<ServeEvent>
        get() {
            check(wireMockServer != null && wireMockServer!!.isRunning) { "Received requests can only be retrieved when Mock Server is running" }
            return wireMockServer!!.allServeEvents.filter { it.request.host == hostname }
        }

    /**
     * Verifies that this Mock Server instance received requests which match the given pattern.
     * @throws IllegalStateException if the HTTP Mock Server is not initialized.
     * @throws VerificationException if Mock Server did not receive the expected number of requests
     */
    fun verify(verification: MockServerVerification) {
        checkNotNull(wireMockServer) { "Mock Server needs to be initialized" }
        val matcher = if (verification.stubName != null) {
            val stub = this.stubs.firstOrNull { it.name == verification.stubName }
            requireNotNull(stub) { "No stub with name ${verification.stubName} exists" }
            stub.request
        } else {
            MockServerStubRequestMatcher(
                hostname = hostname,
                method = verification.method,
                path = verification.path,
                headers = verification.headers,
                queryParameters = verification.queryParameters,
                bodyPatterns = verification.requestBodyPattern?.let { listOf(it) },
            )
        }

        val matchResults = wireMockRequestsReceived.filter { matcher.matches(it.request) }

        if (verification.times != null && verification.times != matchResults.size) {
            throw VerificationException.forTimesNotMatching(name, verification.times, matcher, matchResults, wireMockRequestsReceived)
        } else if (verification.times == null && matchResults.isEmpty()) {
            throw VerificationException.forNotReceivedAtLeastOnce(name, matcher, wireMockRequestsReceived)
        } else if (verification.noOther && wireMockRequestsReceived.size != matchResults.size) {
            throw VerificationException.forOtherRequests(name, matcher, matchResults, wireMockRequestsReceived)
        }
    }
}
