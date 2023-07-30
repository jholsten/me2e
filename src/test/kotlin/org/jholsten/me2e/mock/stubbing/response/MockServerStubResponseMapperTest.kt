package org.jholsten.me2e.mock.stubbing.response

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MockServerStubResponseMapperTest {

    @Test
    fun `Mapping response without body to response definition should succeed`() {
        val stubResponse = MockServerStubResponse(
            code = 200,
            headers = mapOf("Content-Length" to listOf("0")),
        )

        val response = MockServerStubResponseMapper.toWireMockResponseDefinition(stubResponse).build()

        assertEquals(200, response.status)
        assertEquals(listOf("0"), response.headers.getHeader("Content-Length").values())
        assertNull(response.body)
    }

    @Test
    fun `Mapping response with string body to response definition should succeed`() {
        val stubResponse = MockServerStubResponse(
            code = 200,
            headers = mapOf("Content-Type" to listOf("text/plain")),
            body = MockServerStubResponseBody(
                stringContent = "ABC",
            ),
        )

        val response = MockServerStubResponseMapper.toWireMockResponseDefinition(stubResponse).build()

        assertEquals(200, response.status)
        assertEquals(listOf("text/plain"), response.headers.getHeader("Content-Type").values())
        assertEquals("ABC", response.body)
        assertEquals("ABC", response.textBody)
    }

    @Test
    fun `Mapping response with json body to response definition should succeed`() {
        val responseBodyContent = parseJsonNode("{\"name\": \"value\"}")
        val stubResponse = MockServerStubResponse(
            code = 200,
            headers = mapOf("Content-Type" to listOf("application/json")),
            body = MockServerStubResponseBody(
                jsonContent = responseBodyContent,
            ),
        )

        val response = MockServerStubResponseMapper.toWireMockResponseDefinition(stubResponse).build()

        assertEquals(200, response.status)
        assertEquals(listOf("application/json"), response.headers.getHeader("Content-Type").values())
        assertEquals(responseBodyContent, response.jsonBody)
        assertEquals("{\"name\":\"value\"}", response.reponseBody.asString())
    }

    @Test
    fun `Mapping response with base64 body to response definition should succeed`() {
        val stubResponse = MockServerStubResponse(
            code = 200,
            headers = mapOf("Content-Type" to listOf("application/json")),
            body = MockServerStubResponseBody(
                base64Content = "YWJj",
            ),
        )

        val response = MockServerStubResponseMapper.toWireMockResponseDefinition(stubResponse).build()

        assertEquals(200, response.status)
        assertEquals(listOf("application/json"), response.headers.getHeader("Content-Type").values())
        assertEquals("YWJj", response.base64Body)
        assertEquals("YWJj", response.reponseBody.asBase64())
        assertEquals("abc", response.reponseBody.asString())
    }

    private fun parseJsonNode(value: String): JsonNode {
        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        return mapper.readTree(value)
    }
}
