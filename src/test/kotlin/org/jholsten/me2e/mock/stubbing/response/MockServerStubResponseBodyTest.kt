package org.jholsten.me2e.mock.stubbing.response

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlin.test.*

internal class MockServerStubResponseBodyTest {

    @Test
    fun `Deserializing mock server stub response body should set correct properties`() {
        val value = """
            {
                "string-content": "abc",
                "json-content": {
                    "name": "value"
                },
                "base64-content": "ABC"
            }
        """.trimIndent()

        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val result = mapper.readValue(value, MockServerStubResponseBody::class.java)

        assertEquals("abc", result.stringContent)
        assertEquals(JsonNodeFactory.instance.objectNode().put("name", "value"), result.jsonContent)
        assertEquals("ABC", result.base64Content)
    }
}
