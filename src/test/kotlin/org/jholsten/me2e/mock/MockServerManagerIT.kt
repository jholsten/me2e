package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
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

@WireMockTest(proxyMode = true)
internal class MockServerManagerIT {

    private val client = HttpClientBuilder.create()
        .useSystemProperties()
        .build()

    @AfterTest
    fun afterTest() {
        JvmProxyConfigurer.restorePrevious()
    }

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
        val response: CloseableHttpResponse? = client.execute(request)

        assertEquals(200, response?.statusLine?.statusCode)
        assertNotNull(response?.entity)
        assertEquals(responseBodyContent, encodeResponseBody(response?.entity))
        assertEquals("application/json", response?.getFirstHeader("Content-Type")?.value)
        assertEquals(1, exampleServer.requestsReceived.size)
        assertEquals(0, googleServer.requestsReceived.size)
        assertEquals(expectedReceivedRequest.url, exampleServer.requestsReceived.first().url)
        assertEquals(expectedReceivedRequest.method, exampleServer.requestsReceived.first().method)
        assertEquals(expectedReceivedRequest.body?.asString(), exampleServer.requestsReceived.first().body?.asString())
        assertEquals("application/json", exampleServer.requestsReceived.first().body?.contentType?.value)
        assertTrue(manager.isRunning)
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
