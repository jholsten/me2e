package org.jholsten.me2e.mock.stubbing.request

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.Metadata
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.matching.MatchResult
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension
import com.github.tomakehurst.wiremock.matching.WeightedMatchResult.weight

internal class MockServerStubRequestMapper private constructor() {
    companion object {
        /**
         * Maps stub request to equivalent stub for WireMock. Uses weighted matching to show nearly missed stubs.
         * @see [com.github.tomakehurst.wiremock.matching.RequestPattern]
         */
        @JvmStatic
        fun toWireMockStubRequestMatcher(stubRequest: MockServerStubRequestMatcher): MappingBuilder {
            val matcher = object : RequestMatcherExtension() {
                override fun match(request: Request, parameters: Parameters): MatchResult {
                    val results = listOf(
                        stubRequest.hostnameMatches(request.host) to 25.0,
                        stubRequest.pathMatches(request.url) to 10.0,
                        stubRequest.methodMatches(request.method) to 10.0,
                        stubRequest.headersMatch(request.headers) to 1.0,
                        stubRequest.queryParametersMatch(request) to 1.0,
                        stubRequest.bodyPatternsMatch(request) to 1.0,
                    )

                    return MatchResult.aggregateWeighted(results.map {
                        weight(if (it.first) MatchResult.exactMatch() else MatchResult.noMatch(), it.second)
                    })
                }
            }

            return WireMock.requestMatching(matcher).withMetadata(Metadata(mapOf("matcher" to stubRequest)))
        }
    }
}
