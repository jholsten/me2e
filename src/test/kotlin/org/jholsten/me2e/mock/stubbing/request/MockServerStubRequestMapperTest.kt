package org.jholsten.me2e.mock.stubbing.request

import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper.Companion.METADATA_MATCHER_KEY
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMapper.Companion.METADATA_MOCK_SERVER_NAME_KEY
import org.jholsten.me2e.mock.verification.MatchResult
import kotlin.test.*

internal class MockServerStubRequestMapperTest {

    @Test
    fun `Mapping request with all patterns matching should return exact match`() {
        val stub = mockk<MockServerStubRequestMatcher>()
        every { stub.hostnameMatches(any()) } returns MatchResult(true)
        every { stub.pathMatches(any()) } returns MatchResult(true)
        every { stub.methodMatches(any()) } returns MatchResult(true)
        every { stub.headersMatch(any()) } returns MatchResult(true)
        every { stub.queryParametersMatch(any()) } returns MatchResult(true)
        every { stub.bodyPatternsMatch(any()) } returns MatchResult(true)

        val request = mockk<Request>()
        every { request.host } returns "example.com"
        every { request.url } returns "/search"
        every { request.method } returns RequestMethod.GET
        every { request.headers } returns HttpHeaders()

        val mapping = MockServerStubRequestMapper.toWireMockStubRequestMatcher("example-service", stub).build()

        val matchResult = mapping.request.match(request)
        assertTrue(matchResult.isExactMatch)
        assertEquals(0.0, matchResult.distance)
        assertEquals(stub, mapping.metadata[METADATA_MATCHER_KEY])
        assertEquals("example-service", mapping.metadata[METADATA_MOCK_SERVER_NAME_KEY])
        verify { stub.hostnameMatches("example.com") }
        verify { stub.pathMatches("/search") }
        verify { stub.methodMatches(RequestMethod.GET) }
        verify { stub.headersMatch(HttpHeaders()) }
        verify { stub.queryParametersMatch(request) }
        verify { stub.bodyPatternsMatch(request) }
    }

    @Test
    fun `Mapping request with no patterns matching should return no match`() {
        val stub = mockk<MockServerStubRequestMatcher>()
        every { stub.hostnameMatches(any()) } returns MatchResult(false)
        every { stub.pathMatches(any()) } returns MatchResult(false)
        every { stub.methodMatches(any()) } returns MatchResult(false)
        every { stub.headersMatch(any()) } returns MatchResult(false)
        every { stub.queryParametersMatch(any()) } returns MatchResult(false)
        every { stub.bodyPatternsMatch(any()) } returns MatchResult(false)

        val request = mockk<Request>()
        every { request.host } returns "example.com"
        every { request.url } returns "/search"
        every { request.method } returns RequestMethod.GET
        every { request.headers } returns HttpHeaders()

        val mapping = MockServerStubRequestMapper.toWireMockStubRequestMatcher("example-service", stub).build()

        val matchResult = mapping.request.match(request)
        assertFalse(matchResult.isExactMatch)
        assertTrue(matchResult.distance > 0.0)
        assertEquals(stub, mapping.metadata[METADATA_MATCHER_KEY])
        assertEquals("example-service", mapping.metadata[METADATA_MOCK_SERVER_NAME_KEY])
        verify { stub.hostnameMatches("example.com") }
        verify { stub.pathMatches("/search") }
        verify { stub.methodMatches(RequestMethod.GET) }
        verify { stub.headersMatch(HttpHeaders()) }
        verify { stub.queryParametersMatch(request) }
        verify { stub.bodyPatternsMatch(request) }
    }
}
