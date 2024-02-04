package org.jholsten.me2e.mock.stubbing.response

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse

internal class MockServerStubResponseMapper private constructor() {
    companion object {
        /**
         * Maps the given stub response to Wire Mock equivalent.
         */
        @JvmSynthetic
        fun toWireMockResponseDefinition(stubResponse: MockServerStubResponse): ResponseDefinitionBuilder {
            val builder = aResponse().withStatus(stubResponse.code)
            for (header in stubResponse.headers) {
                header.value.forEach { builder.withHeader(header.key, it) }
            }
            if (stubResponse.body != null) {
                when {
                    stubResponse.body.stringContent != null -> builder.withBody(stubResponse.body.stringContent)
                    stubResponse.body.jsonContent != null -> builder.withJsonBody(stubResponse.body.jsonContent)
                    stubResponse.body.base64Content != null -> builder.withBase64Body(stubResponse.body.base64Content)
                }
            }

            return builder
        }
    }
}
