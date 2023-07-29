package org.jholsten.me2e.mock.stubbing.request

import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import org.jholsten.me2e.request.model.HttpMethod
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class MockServerStubRequestTest {

    @Test
    fun `Request with the same method and url should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET
        )
        val request = wireMockRequest(url = "/search", method = RequestMethod.GET)

        assertTrue(matcher.methodMatches(request.method))
        assertTrue(matcher.pathMatches(request.url))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request with another method and url should not match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET
        )
        val request = wireMockRequest(url = "/upload", method = RequestMethod.POST)

        assertFalse(matcher.methodMatches(request.method))
        assertFalse(matcher.pathMatches(request.url))
        assertFalse(matcher.matches(request))
    }

    @Test
    fun `Request with the same headers should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to StringMatcher(equals = "application/json")),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
            headers = HttpHeaders(HttpHeader("Content-Type", "application/json"))
        )

        assertTrue(matcher.headersMatch(request.headers))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request with empty headers should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf(),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
            headers = HttpHeaders(HttpHeader("Content-Type", "application/json"))
        )

        assertTrue(matcher.headersMatch(request.headers))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request without headers should not match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to StringMatcher(equals = "application/json")),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
        )

        assertFalse(matcher.headersMatch(request.headers))
        assertFalse(matcher.matches(request))
    }

    @Test
    fun `Request with other headers should not match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to StringMatcher(equals = "application/json")),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
            headers = HttpHeaders(HttpHeader("Content-Type", "text/plain"))
        )

        assertFalse(matcher.headersMatch(request.headers))
        assertFalse(matcher.matches(request))
    }

    @Test
    fun `Request with the same query parameters should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf("name" to StringMatcher(equals = "dog"))
        )
        val request = wireMockRequest(
            url = "/search?name=dog",
            method = RequestMethod.GET,
        )

        assertTrue(matcher.queryParametersMatch(request))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request with empty query parameters should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf()
        )
        val request = wireMockRequest(
            url = "/search?name=dog",
            method = RequestMethod.GET,
        )

        assertTrue(matcher.queryParametersMatch(request))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request without query parameters should not match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf("name" to StringMatcher(equals = "dog"))
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
        )

        assertFalse(matcher.queryParametersMatch(request))
        assertFalse(matcher.matches(request))
    }

    @Test
    fun `Request with other query parameters should not match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf("name" to StringMatcher(equals = "dog"))
        )
        val request = wireMockRequest(
            url = "/search?name=cat",
            method = RequestMethod.GET,
        )

        assertFalse(matcher.queryParametersMatch(request))
        assertFalse(matcher.matches(request))
    }

    @Test
    fun `Request with the same body should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/upload"),
            method = HttpMethod.POST,
            bodyPatterns = listOf(
                StringMatcher(equals = "Hello World")
            )
        )
        val request = wireMockRequest(
            url = "/upload",
            method = RequestMethod.POST,
            body = "Hello World",
        )

        assertTrue(matcher.bodyPatternsMatch(request))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request with multiple body patterns should match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/upload"),
            method = HttpMethod.POST,
            bodyPatterns = listOf(
                StringMatcher(contains = "Hello"),
                StringMatcher(contains = "World")
            )
        )
        val request = wireMockRequest(
            url = "/upload",
            method = RequestMethod.POST,
            body = "Hello World",
        )

        assertTrue(matcher.bodyPatternsMatch(request))
        assertTrue(matcher.matches(request))
    }

    @Test
    fun `Request with one of multiple body patterns not matching should not match`() {
        val matcher = MockServerStubRequest(
            path = StringMatcher(equals = "/upload"),
            method = HttpMethod.POST,
            bodyPatterns = listOf(
                StringMatcher(contains = "Hello"),
                StringMatcher(contains = "No")
            )
        )
        val request = wireMockRequest(
            url = "/upload",
            method = RequestMethod.POST,
            body = "Hello World",
        )

        assertFalse(matcher.bodyPatternsMatch(request))
        assertFalse(matcher.matches(request))
    }

    private fun wireMockRequest(url: String, method: RequestMethod, headers: HttpHeaders = HttpHeaders.noHeaders(), body: String? = null): Request {
        return LoggedRequest(
            url,
            "http://localhost:8080$url",
            method,
            "127.0.0.1",
            headers,
            mapOf(),
            false,
            Date(),
            body?.let { Base64.getEncoder().encodeToString(body.toByteArray()) },
            null,
            listOf(),
            "http/1.1",
        )
    }
}
