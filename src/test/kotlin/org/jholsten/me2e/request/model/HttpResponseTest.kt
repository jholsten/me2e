package org.jholsten.me2e.request.model

import org.jholsten.util.RecursiveComparison
import kotlin.test.*

internal class HttpResponseTest {

    @Test
    fun `Response builder should set the configured properties`() {
        val httpRequest = httpRequest()
        val body = HttpResponseBody(
            contentType = MediaType("application/json"),
            content = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
        )
        val httpResponse = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withProtocol("http/1.1")
            .withMessage("Hello World")
            .withCode(200)
            .withHeaders(mapOf("Name" to listOf("Value")))
            .withBody(body)
            .build()

        assertEquals(httpRequest, httpResponse.request)
        assertEquals("http/1.1", httpResponse.protocol)
        assertEquals("Hello World", httpResponse.message)
        assertEquals(200, httpResponse.code)
        assertEquals(mapOf("Name" to listOf("Value")), httpResponse.headers)
        assertEquals(body, httpResponse.body)
    }

    @Test
    fun `Response builder should add multiple headers`() {
        val httpRequest = httpRequest()
        val httpResponse = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withProtocol("http/1.1")
            .withMessage("Hello World")
            .withCode(200)
            .addHeader("Name", "Value")
            .addHeader("Authorization", "Bearer 123")
            .build()

        assertEquals(httpRequest, httpResponse.request)
        assertEquals("http/1.1", httpResponse.protocol)
        assertEquals("Hello World", httpResponse.message)
        assertEquals(200, httpResponse.code)
        assertEquals(mapOf("Name" to listOf("Value"), "Authorization" to listOf("Bearer 123")), httpResponse.headers)
        assertNull(httpResponse.body)
    }

    @Test
    fun `Response builder should not add the same header value twice`() {
        val httpRequest = httpRequest()
        val httpResponse = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withProtocol("http/1.1")
            .withMessage("Hello World")
            .withCode(200)
            .addHeader("Name", "Value")
            .addHeader("Name", "Value")
            .build()

        assertEquals(httpRequest, httpResponse.request)
        assertEquals("http/1.1", httpResponse.protocol)
        assertEquals("Hello World", httpResponse.message)
        assertEquals(200, httpResponse.code)
        assertEquals(mapOf("Name" to listOf("Value")), httpResponse.headers)
        assertNull(httpResponse.body)
    }

    @Test
    fun `Response builder should add header value for existing key`() {
        val httpRequest = httpRequest()
        val httpResponse = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withProtocol("http/1.1")
            .withMessage("Hello World")
            .withCode(200)
            .addHeader("Name", "Value")
            .addHeader("Name", "AnotherValue")
            .build()

        assertEquals(httpRequest, httpResponse.request)
        assertEquals("http/1.1", httpResponse.protocol)
        assertEquals("Hello World", httpResponse.message)
        assertEquals(200, httpResponse.code)
        assertEquals(mapOf("Name" to listOf("Value", "AnotherValue")), httpResponse.headers)
        assertNull(httpResponse.body)
    }

    @Test
    fun `New builder should copy values from instance`() {
        val httpResponse = HttpResponse(
            request = httpRequest(),
            protocol = "http/1.1",
            message = "Hello World",
            code = 200,
            headers = mapOf("Name" to listOf("Value")),
            body = HttpResponseBody(
                contentType = MediaType("application/json"),
                content = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
            ),
        )

        val newInstance = httpResponse.newBuilder().build()

        RecursiveComparison.assertEquals(httpResponse, newInstance)
        assertNotEquals(httpResponse, newInstance)
    }

    @Test
    fun `Response builder should fail without request`() {
        val builder = HttpResponse.Builder()
            .withProtocol("http/1.1")
            .withMessage("Hello World")
            .withCode(200)
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
    }

    @Test
    fun `Response builder should fail without protocol`() {
        val httpRequest = httpRequest()
        val builder = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withMessage("Hello World")
            .withCode(200)
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
    }

    @Test
    fun `Response builder should fail without message`() {
        val httpRequest = httpRequest()
        val builder = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withProtocol("http/1.1")
            .withCode(200)
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
    }

    @Test
    fun `Response builder should fail without code`() {
        val httpRequest = httpRequest()
        val builder = HttpResponse.Builder()
            .withRequest(httpRequest)
            .withProtocol("http/1.1")
            .withMessage("Hello World")
            .withHeaders(mapOf("Name" to listOf("Value")))

        assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
    }

    private fun httpRequest(): HttpRequest {
        return HttpRequest(
            url = Url("https://google.com/"),
            method = HttpMethod.GET,
            body = null,
        )
    }
}
