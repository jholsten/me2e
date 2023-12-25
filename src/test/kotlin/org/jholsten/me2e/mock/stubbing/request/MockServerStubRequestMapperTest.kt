package org.jholsten.me2e.mock.stubbing.request

import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.*

internal class MockServerStubRequestMapperTest {

    @Test
    fun `Mapping request with all patterns matching should return exact match`() {
        val stub = mockk<MockServerStubRequestMatcher>()
        every { stub.hostnameMatches(any()) } returns true
        every { stub.pathMatches(any()) } returns true
        every { stub.methodMatches(any()) } returns true
        every { stub.headersMatch(any()) } returns true
        every { stub.queryParametersMatch(any()) } returns true
        every { stub.bodyPatternsMatch(any()) } returns true

        val request = mockk<Request>()
        every { request.host } returns "example.com"
        every { request.url } returns "/search"
        every { request.method } returns RequestMethod.GET
        every { request.headers } returns HttpHeaders()

        val mapping = MockServerStubRequestMapper.toWireMockStubRequestMatcher(stub).build()

        val matchResult = mapping.request.match(request)
        assertTrue(matchResult.isExactMatch)
        assertEquals(0.0, matchResult.distance)
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
        every { stub.hostnameMatches(any()) } returns false
        every { stub.pathMatches(any()) } returns false
        every { stub.methodMatches(any()) } returns false
        every { stub.headersMatch(any()) } returns false
        every { stub.queryParametersMatch(any()) } returns false
        every { stub.bodyPatternsMatch(any()) } returns false

        val request = mockk<Request>()
        every { request.host } returns "example.com"
        every { request.url } returns "/search"
        every { request.method } returns RequestMethod.GET
        every { request.headers } returns HttpHeaders()

        val mapping = MockServerStubRequestMapper.toWireMockStubRequestMatcher(stub).build()

        val matchResult = mapping.request.match(request)
        assertFalse(matchResult.isExactMatch)
        assertTrue(matchResult.distance > 0.0)
        verify { stub.hostnameMatches("example.com") }
        verify { stub.pathMatches("/search") }
        verify { stub.methodMatches(RequestMethod.GET) }
        verify { stub.headersMatch(HttpHeaders()) }
        verify { stub.queryParametersMatch(request) }
        verify { stub.bodyPatternsMatch(request) }
    }
}
