package org.jholsten.me2e.mock.stubbing

import com.github.tomakehurst.wiremock.WireMockServer
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseMapper

/**
 * Stub defining how the Mock Server should respond on certain requests.
 */
class MockServerStub(
    /**
     * Optional unique name of this stub.
     */
    val name: String? = null,

    /**
     * Request to which the stub should respond.
     */
    val request: MockServerStubRequestMatcher,

    /**
     * Response to be returned.
     */
    val response: MockServerStubResponse,
) {
    /**
     * Registers this stub at the given wire mock server instance.
     * This results in the instance returning the specified response whenever the request matches the specified stub.
     * @param wireMockServer Wire mock server instance at which stub should be registered
     */
    internal fun registerAt(mockServerName: String, wireMockServer: WireMockServer) {
        val requestMatcher = MockServerStubRequestMapper.toWireMockStubRequestMatcher(mockServerName, this.request)
        val response = MockServerStubResponseMapper.toWireMockResponseDefinition(this.response)

        wireMockServer.stubFor(requestMatcher.willReturn(response))
    }
}
