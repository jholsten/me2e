package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.WireMockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub

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
    // TODO: This is the equivalent to the wire mock server instance (i.e. needs reset, stubFor, getEvents etc.)
    val stubs: List<MockServerStub>

    init {
        this.stubs = listOf() // stubs TODO: Read from file + call stubFor
        val wireMockServer = WireMockServer()
    }
}
