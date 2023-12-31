package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.io.entity.StringEntity
import org.jholsten.me2e.mock.exception.VerificationException
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher.Companion.containing
import org.jholsten.me2e.mock.stubbing.request.StringMatcher.Companion.equalTo
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.mock.verification.MockServerVerification.Companion.receivedRequest
import org.jholsten.me2e.request.model.*
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

class MockServerIT {

    private val client = HttpClientBuilder.create()
        .useSystemProperties()
        .build()

    private val exampleServer = MockServer(
        "example-service", "example.com", listOf(
            MockServerStub(
                request = MockServerStubRequestMatcher(
                    hostname = "example.com",
                    method = HttpMethod.POST,
                    path = StringMatcher(equals = "/search"),
                    queryParameters = mapOf("id" to StringMatcher(equals = "123"))
                ),
                response = MockServerStubResponse(
                    code = 200,
                    body = MockServerStubResponseBody(
                        stringContent = "A Response",
                    ),
                    headers = mapOf("Content-Type" to listOf("text/plain")),
                )
            ),
            MockServerStub(
                name = "request-stub",
                request = MockServerStubRequestMatcher(
                    hostname = "example.com",
                    method = HttpMethod.GET,
                    path = StringMatcher(equals = "/"),
                ),
                response = MockServerStubResponse(
                    code = 200,
                    body = MockServerStubResponseBody(
                        stringContent = "Some Response"
                    ),
                    headers = mapOf("Content-Type" to listOf("text/plain"))
                )
            )
        )
    )

    private val manager = MockServerManager(mapOf("example-service" to exampleServer))

    @BeforeTest
    fun beforeTest() {
        manager.start()
        assertTrue(manager.isRunning)
        JvmProxyConfigurer.configureFor(manager.httpPort)
    }

    @AfterTest
    fun afterTest() {
        manager.stop()
        assertFalse(manager.isRunning)
        JvmProxyConfigurer.restorePrevious()
    }

    @Test
    fun `Mock server should respond with stubbed response`() {
        val expectedReceivedRequest = HttpRequest(
            url = Url("http://example.com/search?id=123"),
            method = HttpMethod.POST,
        )

        val response = client.execute(HttpPost("http://example.com/search?id=123"))

        assertEquals(200, response?.code)
        assertEquals("A Response", encodeResponseBody(response?.entity))
        assertEquals("text/plain", response?.getFirstHeader("Content-Type")?.value)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(expectedReceivedRequest.url, exampleServer.requestsReceived.first().url)
        assertEquals(expectedReceivedRequest.method, exampleServer.requestsReceived.first().method)
        assertDoesNotThrow {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/search"))
                    .withMethod(HttpMethod.POST)
                    .withQueryParameter("id", equalTo("123"))
                    .andNoOther()
            )
        }
    }

    @Test
    fun `Mock server should respond with stubbed response for request with body`() {
        val expectedReceivedRequest = HttpRequest(
            url = Url("http://example.com/search?id=123"),
            method = HttpMethod.POST,
            body = HttpRequestBody("{\"some-key\": \"some-value\"}", MediaType.JSON_UTF8),
            headers = HttpHeaders(mapOf("header1" to listOf("headerValue"))),
        )

        val request = HttpPost("http://example.com/search?id=123")
        request.entity = StringEntity("{\"some-key\": \"some-value\"}", ContentType.APPLICATION_JSON)
        request.setHeader("header1", "headerValue")
        val response = client.execute(request)

        assertEquals(200, response?.code)
        assertEquals("A Response", encodeResponseBody(response?.entity))
        assertEquals("text/plain", response?.getFirstHeader("Content-Type")?.value)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(expectedReceivedRequest.url, exampleServer.requestsReceived.first().url)
        assertEquals(expectedReceivedRequest.method, exampleServer.requestsReceived.first().method)
        assertEquals(expectedReceivedRequest.body?.asString(), exampleServer.requestsReceived.first().body?.asString())
        assertDoesNotThrow {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/search"))
                    .withMethod(HttpMethod.POST)
                    .withQueryParameter("id", equalTo("123"))
                    .withRequestBody(containing("some-key").and(containing("some-value")))
                    .withHeader("header1", equalTo("headerValue"))
                    .andNoOther()
            )
        }
    }

    @Test
    fun `Mock server verification should succeed for named stub`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            exampleServer.verify(
                receivedRequest(1)
                    .matchingStub("request-stub")
            )
        }
    }

    @Test
    fun `Mock server verification should fail for non-existing named stub`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertFailsWith<IllegalArgumentException> {
            exampleServer.verify(
                receivedRequest(1)
                    .matchingStub("non-existing")
            )
        }
    }

    @Test
    fun `Mock server verification should fail if expected request was not received`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/"))
                    .withMethod(HttpMethod.GET)
                    .andNoOther()
            )
        }
        assertFailsWith<VerificationException> {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/something-else"))
                    .withMethod(HttpMethod.GET)
            )
        }
    }

    @Test
    fun `Mock server verification should fail if expected request was not received at least once`() {
        assertEquals(0, exampleServer.requestsReceived.size)
        assertFailsWith<VerificationException> {
            exampleServer.verify(
                receivedRequest()
                    .withPath(equalTo("/"))
                    .withMethod(HttpMethod.GET)
            )
        }
    }

    @Test
    fun `Mock server verification should fail if other requests were received`() {
        val response1 = client.execute(HttpGet("http://example.com"))
        val response2 = client.execute(HttpPost("http://example.com/search?id=123"))

        assertEquals(200, response1?.code)
        assertEquals(200, response2?.code)
        assertEquals(2, exampleServer.requestsReceived.size)
        val request1Verification = receivedRequest()
            .withPath(equalTo("/"))
            .withMethod(HttpMethod.GET)
        val request2Verification = receivedRequest()
            .withPath(equalTo("/search"))
            .withMethod(HttpMethod.POST)
            .withQueryParameter("id", equalTo("123"))

        assertDoesNotThrow { exampleServer.verify(request1Verification) }
        assertDoesNotThrow { exampleServer.verify(request2Verification) }
        assertFailsWith<VerificationException> {
            exampleServer.verify(request1Verification.andNoOther())
        }
        assertFailsWith<VerificationException> {
            exampleServer.verify(request2Verification.andNoOther())
        }
    }

    @Test
    fun `Mock server should respond with 404 if no stub mapping matches`() {
        val response = client.execute(HttpGet("http://example.com/not-stubbed"))

        assertEquals(404, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/not-stubbed"))
                    .withMethod(HttpMethod.GET)
                    .andNoOther()
            )
        }
    }

    @Test
    fun `Resetting mock server should reset stubs and requests`() {
        val response = client.execute(HttpGet("http://example.com"))

        assertEquals(200, response.code)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertDoesNotThrow {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/"))
                    .withMethod(HttpMethod.GET)
            )
        }

        exampleServer.reset()

        assertEquals(0, exampleServer.requestsReceived.size)
        assertFailsWith<VerificationException> {
            exampleServer.verify(
                receivedRequest(1)
                    .withPath(equalTo("/"))
                    .withMethod(HttpMethod.GET)
            )
        }
    }

    private fun encodeResponseBody(body: HttpEntity?): String? {
        return body?.content?.let { String(it.readAllBytes()) }
    }
}
