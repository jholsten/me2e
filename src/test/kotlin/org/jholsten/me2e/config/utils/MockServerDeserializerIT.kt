package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.*

class MockServerDeserializerIT {

    private val yamlMapper = YAMLMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun `Deserializing mock server should succeed`() {
        val contents = """
            name: mock-server
            hostname: example.com
            stubs:
                - request_stub.json
        """.trimIndent()
        val mockServer = yamlMapper.readValue(contents, MockServer::class.java)

        val expected = MockServer(
            name = "mock-server",
            hostname = "example.com",
            stubs = listOf(
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
                            jsonContent = JsonNodeFactory.instance.objectNode()
                                .put("id", 123)
                                .set<ObjectNode>("items", JsonNodeFactory.instance.arrayNode()
                                    .add(JsonNodeFactory.instance.objectNode().put("name", "A").put("value", 42))
                                    .add(JsonNodeFactory.instance.objectNode().put("name", "B").put("value", 1))
                                ),
                        ),
                        headers = mapOf("Content-Type" to listOf("application/json")),
                    )
                )
            )
        )

        RecursiveComparison.assertEquals(expected, mockServer)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        """
            name: mock-server
            hostname: example.com
        """,
        """
            name: mock-server
            hostname: example.com
            stubs:
        """,
    ])
    fun `Deserializing mock server without stubs should succeed`(contents: String) {
        val mockServer = yamlMapper.readValue(contents, MockServer::class.java)

        val expected = MockServer(
            name = "mock-server",
            hostname = "example.com",
            stubs = listOf()
        )

        RecursiveComparison.assertEquals(expected, mockServer)
    }

    @Test
    fun `Deserializing mock server with non-existing stub file should fail`() {
        val contents = """
            name: mock-server
            hostname: example.com
            stubs:
                - non-existing.json
        """.trimIndent()

        assertFailsWith<JsonMappingException> {
            yamlMapper.readValue(contents, MockServer::class.java)
        }
    }

    @Test
    fun `Deserializing mock server with invalid stub definition should fail`() {
        val contents = """
            name: mock-server
            hostname: example.com
            stubs:
                - test-file.txt
        """.trimIndent()

        assertFailsWith<InvalidFormatException> {
            yamlMapper.readValue(contents, MockServer::class.java)
        }
    }
}
