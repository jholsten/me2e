package org.jholsten.me2e.mock.stubbing.response

/**
 * Definition of the response to be returned by a Mock Server.
 */
class MockServerStubResponse(
    /**
     * HTTP status code to return.
     */
    val code: Int,

    /**
     * Headers of the response to return.
     */
    val headers: Map<String, List<String>> = mapOf(),

    /**
     * Body of the stubbed response.
     */
    val body: MockServerStubResponseBody? = null,
)
