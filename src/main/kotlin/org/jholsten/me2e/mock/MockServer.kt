package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.config.parser.deserializer.MockServerDeserializer
import org.jholsten.me2e.mock.verification.exception.VerificationException
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper.Companion.METADATA_MOCK_SERVER_NAME_KEY
import org.jholsten.me2e.mock.verification.ExpectedRequest
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpRequest


/**
 * Service representing one third party service to be mocked.
 * Several stubs can be registered for one Mock Server, which are used to define which requests the server should respond
 * to and how. The [hostname] is used to specify the name of the host of the third party service to be mocked.
 */
@JsonDeserialize(using = MockServerDeserializer::class)
class MockServer internal constructor(
    /**
     * Unique name of this Mock Server.
     */
    val name: String,

    /**
     * Hostname of the service to be mocked.
     * Is used to match incoming requests to the corresponding Mock Server instance.
     */
    val hostname: String,

    /**
     * Definition of stubs for this Mock Server.
     * Specifies which requests this server should respond to and how.
     */
    val stubs: List<MockServerStub> = listOf(),
) {

    /**
     * Returns whether the Mock Server is currently up and running.
     */
    val isRunning: Boolean
        get() = wireMockServer?.isRunning == true

    /**
     * Mock Server instance that handles incoming requests.
     * All Mock Server instances share one WireMock server.
     */
    private var wireMockServer: WireMockServer? = null

    /**
     * Returns all requests that this Mock Server received sorted by their timestamp.
     * @throws IllegalStateException if the Mock Server is not initialized or not running.
     */
    val requestsReceived: List<HttpRequest>
        get() {
            val events = wireMockRequestsReceived.toMutableList()
            events.sortBy { it.request.loggedDate }
            return events.map { HttpRequestMapper.INSTANCE.toInternalDto(it.request) }
        }

    /**
     * Returns all requests as [ServeEvent] instances that this Mock Server received.
     * @throws IllegalStateException if the Mock Server is not initialized or not running.
     */
    private val wireMockRequestsReceived: List<ServeEvent>
        get() {
            assertThatMockServerIsInitializedAndRunning()
            return wireMockServer!!.allServeEvents.filter { it.request.host == hostname }
        }

    /**
     * Resets all captured requests for this Mock Server. As a result, the list of [requestsReceived] will be empty.
     * @throws IllegalStateException if the Mock Server is not initialized.
     */
    fun reset() {
        assertThatMockServerIsInitialized()
        val metadataMatcher = WireMock.matchingJsonPath("$.$METADATA_MOCK_SERVER_NAME_KEY", WireMock.equalTo(name))
        wireMockServer!!.removeServeEventsForStubsMatchingMetadata(metadataMatcher)
    }

    /**
     * Callback function to execute when the WireMock server has been started.
     * Sets the reference to the server for this instance.
     * @param wireMockServer Reference to the WireMock server which has been started.
     */
    @JvmSynthetic
    internal fun initialize(wireMockServer: WireMockServer) {
        this.wireMockServer = wireMockServer
    }

    /**
     * Registers all stubs defined for this Mock Server.
     * This leads to the Mock Server responding with the specified response whenever the request matches the corresponding stub.
     * @throws IllegalStateException if the Mock Server is not initialized.
     */
    @JvmSynthetic
    internal fun registerStubs() {
        assertThatMockServerIsInitialized()
        for (stub in this.stubs) {
            stub.registerAt(name, wireMockServer!!)
        }
    }

    /**
     * Verifies that this Mock Server instance received requests which match the given pattern.
     * @param times Number of times that the Mock Server should have received the expected request.
     * If set to `null`, it is verified that the Mock Server received the specified request at least once.
     * @param expectedRequest Expectation for the request that the Mock Server should have received.
     * @throws IllegalStateException if the Mock Server is not initialized.
     * @throws VerificationException if Mock Server did not receive the expected number of requests.
     */
    @JvmSynthetic
    internal fun verify(times: Int?, expectedRequest: ExpectedRequest) {
        assertThatMockServerIsInitialized()
        val matchResults = wireMockRequestsReceived.filter { expectedRequest.matches(this, it.request) }

        if (times != null && times != matchResults.size) {
            throw VerificationException.forTimesNotMatching(name, times, expectedRequest, matchResults, wireMockRequestsReceived)
        } else if (times == null && matchResults.isEmpty()) {
            throw VerificationException.forNotReceivedAtLeastOnce(name, expectedRequest, wireMockRequestsReceived)
        } else if (expectedRequest.noOther && wireMockRequestsReceived.size != matchResults.size) {
            throw VerificationException.forOtherRequests(name, expectedRequest, matchResults, wireMockRequestsReceived)
        }
    }

    private fun assertThatMockServerIsInitialized() {
        checkNotNull(wireMockServer) { "Mock Server needs to be initialized. Are you sure that the Mock Server has started?" }
    }

    private fun assertThatMockServerIsInitializedAndRunning() {
        check(wireMockServer != null && wireMockServer!!.isRunning) {
            "Mock Server needs to be initialized and running. " +
                "Are you sure that the Mock Server has started?"
        }
    }
}
