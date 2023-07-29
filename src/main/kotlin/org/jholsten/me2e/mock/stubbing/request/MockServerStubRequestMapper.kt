package org.jholsten.me2e.mock.stubbing.request

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.ValueMatcher

internal class MockServerStubRequestMapper private constructor() {
    companion object {
        /**
         * Maps stub request to equivalent stub for WireMock.
         */
        @JvmStatic
        fun toWireMockStubRequestMatcher(stubRequest: MockServerStubRequest): MappingBuilder {
            val matcher = object : ValueMatcher<Request> {
                override fun match(request: Request): MatchResult {
                    // TODO: Introduce partial matches (see [com.github.tomakehurst.wiremock.matching.RequestPattern])
                    return if (stubRequest.matches(request)) {
                        MatchResult.exactMatch()
                    } else {
                        MatchResult.noMatch()
                    }
                }
            }
            return WireMock.requestMatching(matcher)
        }
    }
}
