package org.jholsten.me2e.mock.stubbing.response

/**
 * Definition of the response to be returned by the mocked web server.
 */
class MockServerStubResponse(
    /**
     * HTTP status code to return
     */
    val code: Int,

    /**
     * Headers of the response
     */
    val headers: Map<String, List<String>> = mapOf(),

    /**
     * Body of the stubbed response
     */
    val body: MockServerStubResponseBody? = null,
)
