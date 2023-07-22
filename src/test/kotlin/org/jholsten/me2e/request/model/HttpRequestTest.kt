package org.jholsten.me2e.request.model

import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
    fun `Request builder should set the configured properties`() {
        val body = HttpRequestBody("content", MediaType.TEXT_PLAIN_UTF8)
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(HttpMethod.POST)
            .withHeaders(mapOf("Authorization" to listOf("Bearer 123")))
            .withBody(body)
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.POST, request.method)
        assertEquals(mapOf("Authorization" to listOf("Bearer 123")), request.headers)
        assertEquals(body, request.body)
    }

    @Test
    fun `Request builder should add multiple headers`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(HttpMethod.GET)
            .addHeader("Name", "Value")
            .addHeader("Authorization", "Bearer 123")
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf("Name" to listOf("Value"), "Authorization" to listOf("Bearer 123")), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `Request builder should not add the same header value twice`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(HttpMethod.GET)
            .addHeader("Name", "Value")
            .addHeader("Name", "Value")
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf("Name" to listOf("Value")), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `Request builder should add header value for existing key`() {
        val request = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(HttpMethod.GET)
            .addHeader("Name", "Value")
            .addHeader("Name", "AnotherValue")
            .build()

        assertEquals("https://google.com/search", request.url)
        assertEquals(HttpMethod.GET, request.method)
        assertEquals(mapOf("Name" to listOf("Value", "AnotherValue")), request.headers)
        assertNull(request.body)
    }

    @Test
    fun `New builder should copy values from instance`() {
        val request = HttpRequest(
            url = "https://google.com/",
            method = HttpMethod.POST,
            headers = mapOf("Name" to listOf("Value")),
            body = HttpRequestBody("content", MediaType.TEXT_PLAIN_UTF8),
        )

        val newInstance = request.newBuilder().build()
        RecursiveComparison.assertEquals(request, newInstance)
        assertNotEquals(request, newInstance)
    }

    @Test
    fun `Request builder should fail without url`() {
        val builder = HttpRequest.Builder()
            .withMethod(HttpMethod.GET)
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertThrowsExactly(IllegalArgumentException::class.java) {
            builder.build()
        }
    }

    @Test
    fun `Request builder should fail without method`() {
        val builder = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertThrowsExactly(IllegalArgumentException::class.java) {
            builder.build()
        }
    }

    @ParameterizedTest(name = "[{index}] Request builder with method {0} should fail without body")
    @EnumSource(HttpMethod::class, names = ["POST", "PUT", "PATCH"])
    fun `Request builder should fail without body for required body methods`(method: HttpMethod) {
        val builder = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(method)
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertThrowsExactly(IllegalArgumentException::class.java) {
            builder.build()
        }
    }

    @ParameterizedTest(name = "[{index}] Request builder with method {0} should fail with body")
    @EnumSource(HttpMethod::class, names = ["GET", "HEAD"])
    fun `Request builder should fail with body for methods that do not allow body`(method: HttpMethod) {
        val builder = HttpRequest.Builder()
            .withUrl("https://google.com", "/search")
            .withMethod(method)
            .withHeaders(mapOf("Name" to listOf("Value")))
            .withBody(HttpRequestBody("content", MediaType.TEXT_PLAIN_UTF8))

        assertThrowsExactly(IllegalArgumentException::class.java) {
            builder.build()
        }
    }
}
