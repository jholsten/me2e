package org.jholsten.me2e.mock.stubbing.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * Definition of the response body of a stubbed response.
 */
class MockServerStubResponseBody(
    /**
     * Literal text content of the response body
     */
    @JsonProperty("string-content")
    val stringContent: String? = null,

    /**
     * JSON content of the response body as a dictionary
     */
    @JsonProperty("json-content")
    val jsonContent: JsonNode? = null,

    /**
     * Binary content of the response body encoded as Base 64
     */
    @JsonProperty("base64-content")
    val base64Content: String? = null,
)
