package org.jholsten.me2e.mock

import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.HttpEntity
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher.Companion.equalTo
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.mock.verification.MockServerVerification.Companion.receivedRequest
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
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
                request = MockServerStubRequestMatcher(
                    hostname = "example.com",
                    method = HttpMethod.GET,
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
            url = "http://example.com/search?id=123",
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

    private fun encodeResponseBody(body: HttpEntity?): String? {
        return body?.content?.let { String(it.readAllBytes()) }
    }
}
