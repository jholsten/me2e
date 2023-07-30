package org.jholsten.me2e.mock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequest
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.request.client.OkHttpClient
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.MediaType
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MockServerIT {

    private val client = OkHttpClient.Builder().withBaseUrl("http://localhost:9000").build()

    @Test
    fun `Mock server should respond with stubbed response`() {
        val responseBodyContent = "{\"id\":123,\"items\":[{\"name\":\"A\",\"value\":42},{\"name\":\"B\",\"value\":1}]}"
        val server = MockServer("service", 9000, listOf("request_stub.json"))
        val expectedStub = MockServerStub(
            request = MockServerStubRequest(
                method = HttpMethod.POST,
                path = StringMatcher(equals = "/search"),
                bodyPatterns = listOf(StringMatcher(contains = "\"id\": 123"))
            ),
            response = MockServerStubResponse(
                code = 200,
                body = MockServerStubResponseBody(
                    jsonContent = parseJsonNode(responseBodyContent),
                ),
                headers = mapOf("Content-Type" to listOf("application/json")),
            )
        )
        val expectedReceivedRequest = HttpRequest(
            url = "http://localhost:9000/search",
            method = HttpMethod.POST,
            body = HttpRequestBody("{\"id\": 123}", MediaType.JSON_UTF8),
        )
        server.start()

        val response = client.post("/search", HttpRequestBody("{\"id\": 123}", MediaType.JSON_UTF8))

        assertEquals(200, response.code)
        assertNotNull(response.body)
        assertEquals(responseBodyContent, response.body!!.asString())
        assertEquals(listOf("application/json"), response.headers["content-type"])
        assertEquals(1, server.stubs.size)
        RecursiveComparison.assertEquals(expectedStub, server.stubs.first())
        assertEquals(1, server.getReceivedRequests().size)
        assertEquals(expectedReceivedRequest.url, server.getReceivedRequests().first().url)
        assertEquals(expectedReceivedRequest.method, server.getReceivedRequests().first().method)
        assertEquals(expectedReceivedRequest.body?.asString(), server.getReceivedRequests().first().body?.asString())
        assertEquals("application/json", server.getReceivedRequests().first().body?.contentType?.value)
        assertTrue(server.isRunning())
    }

    private fun parseJsonNode(value: String): JsonNode {
        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        return mapper.readTree(value)
    }
}
