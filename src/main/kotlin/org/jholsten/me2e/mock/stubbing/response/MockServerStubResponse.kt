package org.jholsten.me2e.mock.stubbing.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Definition of the response to be returned by a Mock Server.
 */
class MockServerStubResponse internal constructor(
    /**
     * HTTP status code to return.
     */
    @JsonProperty("status-code")
    val statusCode: Int,

    /**
     * Headers of the response to return.
     */
    val headers: Map<String, List<String>> = mapOf(),

    /**
     * Body of the stubbed response.
     */
    val body: MockServerStubResponseBody? = null,
)
