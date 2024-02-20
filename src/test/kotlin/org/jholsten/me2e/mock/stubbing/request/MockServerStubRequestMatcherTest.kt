package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import org.jholsten.me2e.config.parser.deserializer.MockServerDeserializer
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.util.RecursiveComparison
import java.util.*
import kotlin.test.*

internal class MockServerStubRequestMatcherTest {

    @Test
    fun `Request with the same hostname, method and url should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET
        )
        val request = wireMockRequest(url = "/search", method = RequestMethod.GET)

        assertTrue(matcher.methodMatches(request.method).matches)
        assertTrue(matcher.pathMatches(request.url).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request with another method and url should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET
        )
        val request = wireMockRequest(url = "/upload", method = RequestMethod.POST)

        assertFalse(matcher.methodMatches(request.method).matches)
        assertFalse(matcher.pathMatches(request.url).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Request with another url should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.POST
        )
        val request = wireMockRequest(url = "/upload", method = RequestMethod.POST)

        assertTrue(matcher.methodMatches(request.method).matches)
        assertFalse(matcher.pathMatches(request.url).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Request with the same headers should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to StringMatcher(equals = "application/json")),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
            headers = HttpHeaders(HttpHeader("Content-Type", "application/json"))
        )

        assertTrue(matcher.headersMatch(request.headers).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request with empty headers should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf(),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
            headers = HttpHeaders(HttpHeader("Content-Type", "application/json"))
        )

        assertTrue(matcher.headersMatch(request.headers).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request without headers should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to StringMatcher(equals = "application/json")),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
        )

        assertFalse(matcher.headersMatch(request.headers).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Request with other headers should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            headers = mapOf("Content-Type" to StringMatcher(equals = "application/json")),
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
            headers = HttpHeaders(HttpHeader("Content-Type", "text/plain"))
        )

        assertFalse(matcher.headersMatch(request.headers).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Request with the same query parameters should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf("name" to StringMatcher(equals = "dog"))
        )
        val request = wireMockRequest(
            url = "/search?name=dog",
            method = RequestMethod.GET,
        )

        assertTrue(matcher.queryParametersMatch(request).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request with empty query parameters should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf()
        )
        val request = wireMockRequest(
            url = "/search?name=dog",
            method = RequestMethod.GET,
        )

        assertTrue(matcher.queryParametersMatch(request).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request without query parameters should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf("name" to StringMatcher(equals = "dog"))
        )
        val request = wireMockRequest(
            url = "/search",
            method = RequestMethod.GET,
        )

        assertFalse(matcher.queryParametersMatch(request).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Request with other query parameters should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
            path = StringMatcher(equals = "/search"),
            method = HttpMethod.GET,
            queryParameters = mapOf("name" to StringMatcher(equals = "dog"))
        )
        val request = wireMockRequest(
            url = "/search?name=cat",
            method = RequestMethod.GET,
        )

        assertFalse(matcher.queryParametersMatch(request).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Request with the same body should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
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

        assertTrue(matcher.bodyPatternsMatch(request).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request with multiple body patterns should match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
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

        assertTrue(matcher.bodyPatternsMatch(request).matches)
        assertTrue(matcher.matches(request).matches)
    }

    @Test
    fun `Request with one of multiple body patterns not matching should not match`() {
        val matcher = MockServerStubRequestMatcher(
            hostname = "example.com",
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

        assertFalse(matcher.bodyPatternsMatch(request).matches)
        assertFalse(matcher.matches(request).matches)
    }

    @Test
    fun `Deserializing Mock Server stub request should set correct properties`() {
        val value = """
            {
                "method": "GET",
                "path": {
                    "equals": "/search"
                },
                "headers": {
                    "Content-Type": {
                        "equals": "application/json"
                    }
                },
                "query-parameters": {
                    "name": {
                        "equals": "dog"
                    }
                },
                "body-patterns": [
                    {
                        "contains": "id"
                    }
                ]
            }
        """.trimIndent()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
            .setInjectableValues(InjectableValues.Std().addValue(MockServerDeserializer.INJECTABLE_HOSTNAME_FIELD_NAME, "example.com"))
        val result = mapper.readValue(value, MockServerStubRequestMatcher::class.java)

        assertEquals(HttpMethod.GET, result.method)
        assertEquals("example.com", result.hostname)
        RecursiveComparison.assertEquals(StringMatcher(equals = "/search"), result.path)
        RecursiveComparison.assertEquals(mapOf("Content-Type" to StringMatcher(equals = "application/json")), result.headers)
        RecursiveComparison.assertEquals(mapOf("name" to StringMatcher(equals = "dog")), result.queryParameters)
        RecursiveComparison.assertEquals(listOf(StringMatcher(contains = "id")), result.bodyPatterns)
    }

    private fun wireMockRequest(
        url: String,
        method: RequestMethod,
        headers: HttpHeaders = HttpHeaders.noHeaders(),
        body: String? = null
    ): Request {
        return LoggedRequest(
            url,
            "http://example.com$url",
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
