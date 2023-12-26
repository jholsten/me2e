package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.io.entity.StringEntity
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.MediaType
import kotlin.test.*
import kotlin.test.Test

internal class MockServerManagerIT {

    private val client = HttpClientBuilder.create()
        .useSystemProperties()
        .build()

    private val responseBodyContent = "{\"id\":123,\"items\":[{\"name\":\"A\",\"value\":42},{\"name\":\"B\",\"value\":1}]}"

    private val exampleServer = MockServer(
        "example-service", "example.com", listOf(
            MockServerStub(
                request = MockServerStubRequestMatcher(
                    hostname = "example.com",
                    method = HttpMethod.POST,
                    path = StringMatcher(equals = "/search"),
                    bodyPatterns = listOf(StringMatcher(contains = "\"id\": 123")),
                ),
                response = MockServerStubResponse(
                    code = 200,
                    body = MockServerStubResponseBody(
                        jsonContent = parseJsonNode(responseBodyContent),
                    ),
                    headers = mapOf("Content-Type" to listOf("application/json")),
                )
            ),
            MockServerStub(
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

    private val googleServer = MockServer(
        "google-service", "google.com", listOf(
            MockServerStub(
                request = MockServerStubRequestMatcher(
                    hostname = "google.com",
                    method = HttpMethod.GET,
                ),
                response = MockServerStubResponse(
                    code = 200,
                    body = MockServerStubResponseBody(
                        stringContent = "Response from mocked Google API"
                    ),
                    headers = mapOf("Content-Type" to listOf("text/plain"))
                )
            )
        )
    )

    @AfterTest
    fun afterTest() {
        JvmProxyConfigurer.restorePrevious()
    }

    @Test
    fun `Mock server should respond with stubbed response`() {
        val expectedReceivedRequest = HttpRequest(
            url = "http://example.com/search",
            method = HttpMethod.POST,
            body = HttpRequestBody("{\"id\": 123}", MediaType.JSON_UTF8),
        )
        val manager = startManager()

        val request = HttpPost("http://example.com/search")
        request.entity = StringEntity("{\"id\": 123}", ContentType.APPLICATION_JSON)
        val response = client.execute(request)

        assertTrue(manager.isRunning)
        assertEquals(200, response?.code)
        assertNotNull(response?.entity)
        assertEquals(responseBodyContent, encodeResponseBody(response?.entity))
        assertEquals("application/json", response?.getFirstHeader("Content-Type")?.value)
        assertEquals(1, manager.requestsReceived.size)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(0, googleServer.requestsReceived.size)
        assertEquals(expectedReceivedRequest.url, exampleServer.requestsReceived.first().url)
        assertEquals(expectedReceivedRequest.method, exampleServer.requestsReceived.first().method)
        assertEquals(expectedReceivedRequest.body?.asString(), exampleServer.requestsReceived.first().body?.asString())
        assertEquals("application/json", exampleServer.requestsReceived.first().body?.contentType?.value)

        manager.stop()
        assertFalse(manager.isRunning)
    }

    @Test
    fun `Mock server should respond with 404 if no stub mapping matches`() {
        val manager = startManager()

        val request = HttpPost("http://example.com/not-stubbed")
        val response = client.execute(request)

        assertTrue(manager.isRunning)
        assertEquals(404, response?.code)
        assertNotNull(response?.entity)
        assertEquals(1, manager.requestsReceived.size)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(0, googleServer.requestsReceived.size)
        assertNull(exampleServer.requestsReceived.first().body)

        val responseBody = encodeResponseBody(response?.entity)
        assertNotNull(responseBody)
        assertContains(responseBody, "Request was not matched")
        assertEquals("text/plain", response?.getFirstHeader("Content-Type")?.value)

        manager.stop()
        assertFalse(manager.isRunning)
    }

    @Test
    fun `Mock server should respond with 404 if no stubs are registered`() {
        val manager = startManager(mockServers = mapOf())

        val request = HttpGet("http://localhost")
        val response = client.execute(request)

        assertTrue(manager.isRunning)
        assertEquals(404, response?.code)
        assertNotNull(response?.entity)
        assertEquals(1, manager.requestsReceived.size)

        assertEquals(
            "No response could be served as there are no stubs registered for the mock server.",
            encodeResponseBody(response?.entity),
        )
        assertEquals("text/plain", response?.getFirstHeader("Content-Type")?.value)

        manager.stop()
        assertFalse(manager.isRunning)
    }

    @Test
    fun `Resetting mock servers should reset stubs and requests`() {
        val manager = startManager()

        val response1 = client.execute(HttpGet("http://example.com"))
        val response2 = client.execute(HttpGet("http://example.com/not-stubbed"))
        val response3 = client.execute(HttpGet("http://localhost"))

        assertTrue(manager.isRunning)
        assertEquals(200, response1.code)
        assertEquals(404, response2.code)
        assertEquals(404, response3.code)
        assertEquals("Some Response", encodeResponseBody(response1?.entity))
        assertEquals(3, manager.requestsReceived.size)
        assertEquals(2, exampleServer.requestsReceived.size)
        assertEquals(0, googleServer.requestsReceived.size)

        manager.resetAll()
        assertEquals(0, manager.requestsReceived.size)
        assertEquals(0, exampleServer.requestsReceived.size)
        assertEquals(0, googleServer.requestsReceived.size)

        manager.stop()
        assertFalse(manager.isRunning)
    }

    @Test
    fun `Trying to start mock server if port is already in use should throw exception`() {
        val manager1 = MockServerManager(mapOf())
        val manager2 = MockServerManager(mapOf())

        try {
            manager1.start()
            val e = assertFailsWith<ServiceStartupException> { manager2.start() }
            assertEquals("Port 80 is already in use", e.message)
        } finally {
            manager1.stop()
        }
    }

    private fun startManager(
        mockServers: Map<String, MockServer> = mapOf(
            "example-service" to exampleServer,
            "google-service" to googleServer,
        )
    ): MockServerManager {
        val manager = MockServerManager(mockServers)
        manager.start()

        JvmProxyConfigurer.configureFor(manager.httpPort)

        return manager
    }

    private fun encodeResponseBody(body: HttpEntity?): String? {
        val stream = body?.content
        return stream?.let { String(it.readAllBytes()) }
    }

    private fun parseJsonNode(value: String): JsonNode {
        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        return mapper.readTree(value)
    }
}
