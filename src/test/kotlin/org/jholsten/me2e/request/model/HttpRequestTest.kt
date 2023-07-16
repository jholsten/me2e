package org.jholsten.me2e.request.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class HttpRequestTest {

    @Test
    fun `URL with query params should be built correctly`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search", queryParams = mapOf("name" to "dog"))
            .withMethod(HttpMethod.GET)
            .build()

        assertEquals("https://google.com/search?name=dog", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf<String, List<String>>(), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `URL with multiple query params should be built correctly`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search", queryParams = mapOf("name" to "dog", "id" to "123"))
            .withMethod(HttpMethod.GET)
            .build()

        assertEquals("https://google.com/search?name=dog&id=123", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf<String, List<String>>(), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `URL with trailing slashes should be built correctly`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com//", "/search")
            .withMethod(HttpMethod.GET)
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf<String, List<String>>(), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `URL with leading slashes should be built correctly`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "//search")
            .withMethod(HttpMethod.GET)
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf<String, List<String>>(), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `Request with headers should be built correctly`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(HttpMethod.GET)
            .withHeaders(mapOf("Authorization" to listOf("Bearer 123")))
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf("Authorization" to listOf("Bearer 123")), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `Request with body should be built correctly`() {
        val body = HttpRequestBody("content", MediaType.TEXT_PLAIN_UTF8)
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(HttpMethod.GET)
            .withBody(body)
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf<String, List<String>>(), request.headers)
        assertEquals(body, request.body)
    }
}
