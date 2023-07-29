package org.jholsten.me2e.mock.stubbing

import com.github.tomakehurst.wiremock.WireMockServer
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequest
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper

/**
 * Stub defining how the Mock Server should respond on certain requests.
 */
class MockServerStub(
    /**
     * Request to which the stub should respond.
     */
    val request: MockServerStubRequest,
) {
    /**
     * Registers this stub at the given wire mock server instance.
     * This results in the instance returning the specified response whenever the request matches the specified stub.
     */
    internal fun register(wireMockServer: WireMockServer) {
        wireMockServer.stubFor(MockServerStubRequestMapper.toWireMockStubRequestMatcher(request))
        // TODO: Return response
    }
}
