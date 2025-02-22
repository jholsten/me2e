package org.jholsten.me2e.request.assertions

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.assertions.*
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.request.model.*
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

internal class AssertableResponseIT {

    private val expectedBinaryContent = byteArrayOf(
        123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 74, 111, 104, 110, 34, 44, 32, 34, 110, 101, 115, 116, 101, 100, 34, 58,
        32, 123, 34, 107, 101, 121, 49, 34, 58, 32, 34, 118, 97, 108, 117, 101, 49, 34, 44, 32, 34, 107, 101, 121, 50, 34, 58, 32,
        34, 118, 97, 108, 117, 101, 50, 34, 125, 44, 32, 34, 100, 101, 116, 97, 105, 108, 115, 34, 58, 32, 91, 123, 34, 100, 101,
        116, 97, 105, 108, 34, 58, 32, 49, 125, 44, 32, 123, 34, 100, 101, 116, 97, 105, 108, 34, 58, 32, 50, 125, 93, 125,
    )

    private val response = HttpResponse(
        request = HttpRequest(
            url = Url("https://google.com/"),
            method = HttpMethod.GET,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = null,
        ),
        protocol = "http/1.1",
        message = "Some Message",
        statusCode = 200,
        headers = HttpHeaders(
            mapOf(
                "Key1" to listOf("Value1.1", "Value1.2"),
                "Key2" to listOf("Value2"),
            )
        ),
        body = HttpResponseBody(
            contentType = ContentType.JSON_UTF8,
            content = expectedBinaryContent,
        ),
    )

    private val expectedObj = BodyClass(
        name = "John",
        nested = NestedBodyClass(
            key1 = "value1",
            key2 = "value2",
        ),
        details = listOf(
            DetailsBodyClass(detail = 1),
            DetailsBodyClass(detail = 2),
        )
    )

    private val expectedJsonNode = JsonNodeFactory.instance.objectNode()
        .put("name", "John")
        .set<ObjectNode>(
            "nested", JsonNodeFactory.instance.objectNode()
                .put("key1", "value1")
                .put("key2", "value2")
        )
        .set<ObjectNode>(
            "details", JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("detail", 1))
                .add(JsonNodeFactory.instance.objectNode().put("detail", 2))
        )

    private val reducedJsonNode: ObjectNode = expectedJsonNode.deepCopy()

    private val extendedJsonNode: ObjectNode = expectedJsonNode.deepCopy()


    private val filename = "responses/expected_body.json"

    init {
        reducedJsonNode.remove("name")
        extendedJsonNode.put("additional", "value")
    }

    @Test
    fun `Asserting all properties should not throw if assertions are satisfied`() {
        assertDoesNotThrow {
            assertThat(response)
                .statusCode(equalTo(200))
                .protocol(equalTo("http/1.1"))
                .message(equalTo("Some Message"))
                .headers(
                    equalTo(
                        mapOf(
                            "Key1" to listOf("Value1.1", "Value1.2"),
                            "Key2" to listOf("Value2"),
                        )
                    )
                )
                .contentType(equalTo("application/json; charset=utf-8"))
                .body(equalTo("{\"name\": \"John\", \"nested\": {\"key1\": \"value1\", \"key2\": \"value2\"}, \"details\": [{\"detail\": 1}, {\"detail\": 2}]}"))
                .base64Body(equalTo("eyJuYW1lIjogIkpvaG4iLCAibmVzdGVkIjogeyJrZXkxIjogInZhbHVlMSIsICJrZXkyIjogInZhbHVlMiJ9LCAiZGV0YWlscyI6IFt7ImRldGFpbCI6IDF9LCB7ImRldGFpbCI6IDJ9XX0="))
                .jsonBody(containsNode("$.name").withValue(equalTo("John")))
                .jsonBody(containsNode("$.nested.key1").withValue(equalTo("value1")))
                .jsonBody(containsNode("$.nested.key2").withValue(equalTo("value2")))
                .jsonBody(containsNode("$.details[0].detail").withValue(equalTo("1")))
                .jsonBody(containsNode("$.details[1].detail").withValue(equalTo("2")))
                .objectBody(BodyClass::class.java, equalTo(expectedObj))
                .objectBody(object : TypeReference<BodyClass>() {}, equalTo(expectedObj))
                .objectBody<BodyClass>(equalTo(expectedObj))
                .binaryBody(equalTo(expectedBinaryContent))
                .body(equalToContentsFromFile(filename).asString())
                .base64Body(equalToContentsFromFile(filename).asBase64())
                .jsonBody(equalToContentsFromFile(filename).asJson())
                .binaryBody(equalToContentsFromFile(filename).asBinary())
                .objectBody(equalToContentsFromFile(filename).asObject(BodyClass::class.java))
                .objectBody(equalToContentsFromFile(filename).asObject(object : TypeReference<BodyClass>() {}))
                .objectBody<BodyClass>(equalToContentsFromFile(filename).asObject())
        }
    }

    @Test
    fun `Asserting status code should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).statusCode(equalTo(response.statusCode)) }
        assertDoesNotThrow { assertThat(response).statusCode(equalTo(200)) }
        assertDoesNotThrow { assertThat(response).statusCode(lessThan(300)) }
        assertDoesNotThrow { assertThat(response).statusCode(greaterThan(100)) }
        assertDoesNotThrow { assertThat(response).statusCode(between(100, 300)) }
        assertDoesNotThrow { assertThat(response).statusCode(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .statusCode(equalTo(response.statusCode))
                .statusCode(equalTo(200))
                .statusCode(lessThan(300))
                .statusCode(greaterThan(100))
                .statusCode(between(100, 300))
                .statusCode(isNotNull())
        }
    }

    @Test
    fun `Asserting status code should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).statusCode(equalTo(400)) }
        assertFails { assertThat(response).statusCode(lessThan(100)) }
        assertFails { assertThat(response).statusCode(greaterThan(300)) }
        assertFails { assertThat(response).statusCode(between(400, 500)) }
        assertFails { assertThat(response).statusCode(isNull()) }
        assertFails {
            assertThat(response)
                .statusCode(equalTo(400))
                .statusCode(lessThan(100))
                .statusCode(greaterThan(300))
                .statusCode(between(400, 500))
                .statusCode(isNull())
        }
    }

    @Test
    fun `Asserting protocol should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).protocol(equalTo(response.protocol)) }
        assertDoesNotThrow { assertThat(response).protocol(equalTo("http/1.1")) }
        assertDoesNotThrow { assertThat(response).protocol(containsString("http")) }
        assertDoesNotThrow { assertThat(response).protocol(matchesPattern("http.{4}")) }
        assertDoesNotThrow { assertThat(response).protocol(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .protocol(equalTo(response.protocol))
                .protocol(equalTo("http/1.1"))
                .protocol(containsString("http"))
                .protocol(matchesPattern("http.{4}"))
                .protocol(isNotNull())
        }
    }

    @Test
    fun `Asserting protocol should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).protocol(equalTo("http/2.0")) }
        assertFails { assertThat(response).protocol(containsString("ABC")) }
        assertFails { assertThat(response).protocol(matchesPattern("^[A-Z]{4}$")) }
        assertFails { assertThat(response).protocol(isNull()) }
        assertFails {
            assertThat(response)
                .protocol(equalTo("http/2.0"))
                .protocol(containsString("ABC"))
                .protocol(matchesPattern("^[A-Z]{4}$"))
                .protocol(isNull())
        }
    }

    @Test
    fun `Asserting message should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).message(equalTo(response.message)) }
        assertDoesNotThrow { assertThat(response).message(equalTo("Some Message")) }
        assertDoesNotThrow { assertThat(response).message(containsString("Message")) }
        assertDoesNotThrow { assertThat(response).message(matchesPattern("[\\w|\\s]*")) }
        assertDoesNotThrow { assertThat(response).message(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .message(equalTo(response.message))
                .message(equalTo("Some Message"))
                .message(containsString("Message"))
                .message(matchesPattern("[\\w|\\s]*"))
                .message(isNotNull())
        }
    }

    @Test
    fun `Asserting message should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).message(equalTo("Another Message")) }
        assertFails { assertThat(response).message(containsString("Something else")) }
        assertFails { assertThat(response).message(matchesPattern("^[A-Z]{4}$")) }
        assertFails { assertThat(response).message(isNull()) }
        assertFails {
            assertThat(response)
                .message(equalTo("Another Message"))
                .message(containsString("Something else"))
                .message(matchesPattern("^[A-Z]{4}$"))
                .message(isNull())
        }
    }

    @Test
    fun `Asserting headers should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).headers(equalTo(response.headers.entries)) }
        assertDoesNotThrow { assertThat(response).headers(containsKey("Key1").withValue(equalTo("Value1.1"))) }
        assertDoesNotThrow { assertThat(response).headers(containsKey("Key1").withValue(equalTo("Value1.2"))) }
        assertDoesNotThrow { assertThat(response).headers(containsKey("Key1").withValues(listOf("Value1.1", "Value1.2"))) }
        assertDoesNotThrow { assertThat(response).headers(containsKey("Key1")) }
        assertDoesNotThrow { assertThat(response).headers(containsKey("Key2").withValue(equalTo("Value2"))) }
        assertDoesNotThrow { assertThat(response).headers(containsKey("Key2")) }
        assertDoesNotThrow {
            assertThat(response).headers(
                equalTo(
                    mapOf(
                        "Key1" to listOf("Value1.1", "Value1.2"),
                        "Key2" to listOf("Value2"),
                    )
                )
            )
        }
        assertDoesNotThrow { assertThat(response).headers(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .headers(equalTo(response.headers.entries))
                .headers(containsKey("Key1").withValue(equalTo("Value1.1")))
                .headers(containsKey("Key1").withValue(equalTo("Value1.2")))
                .headers(containsKey("Key1").withValues(listOf("Value1.1", "Value1.2")))
                .headers(containsKey("Key1"))
                .headers(containsKey("Key2").withValue(equalTo("Value2")))
                .headers(containsKey("Key2"))
                .headers(isNotNull())
        }
    }

    @Test
    fun `Asserting headers should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).headers(containsKey("Key1").withValue(equalTo("Other"))) }
        assertFails { assertThat(response).headers(containsKey("Key1").withValues(listOf("Other"))) }
        assertFails { assertThat(response).headers(containsKey("Key1").withValues(listOf("Value1.1"))) }
        assertFails { assertThat(response).headers(containsKey("Key3")) }
        assertFails { assertThat(response).headers(equalTo(mapOf())) }
        assertFails { assertThat(response).headers(isNull()) }
        assertFails {
            assertThat(response)
                .headers(containsKey("Key1").withValue(equalTo("Other")))
                .headers(containsKey("Key1").withValues(listOf("Other")))
                .headers(containsKey("Key1").withValues(listOf("Value1.1")))
                .headers(containsKey("Key3"))
                .headers(equalTo(mapOf()))
                .headers(isNull())
        }
    }

    @Test
    fun `Asserting content type should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).contentType(equalTo(response.body?.contentType?.value)) }
        assertDoesNotThrow { assertThat(response).contentType(equalTo("application/json; charset=utf-8")) }
        assertDoesNotThrow { assertThat(response).contentType(containsString("application/json")) }
        assertDoesNotThrow { assertThat(response).contentType(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .contentType(equalTo(response.body?.contentType?.value))
                .contentType(equalTo("application/json; charset=utf-8"))
                .contentType(containsString("application/json"))
                .contentType(isNotNull())
        }
    }

    @Test
    fun `Asserting content type should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).contentType(equalTo("text/plain; charset=utf-8")) }
        assertFails { assertThat(response).contentType(containsString("text/plain")) }
        assertFails { assertThat(response).contentType(isNull()) }
        assertFails {
            assertThat(response)
                .contentType(equalTo("text/plain; charset=utf-8"))
                .contentType(containsString("text/plain"))
                .contentType(isNull())
        }
    }

    @Test
    fun `Asserting body should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).body(equalTo(response.body?.asString())) }
        assertDoesNotThrow { assertThat(response).body(equalTo("{\"name\": \"John\", \"nested\": {\"key1\": \"value1\", \"key2\": \"value2\"}, \"details\": [{\"detail\": 1}, {\"detail\": 2}]}")) }
        assertDoesNotThrow { assertThat(response).body(containsString("name")) }
        assertDoesNotThrow { assertThat(response).body(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .body(equalTo(response.body?.asString()))
                .body(containsString("name"))
                .body(isNotNull())
        }
    }

    @Test
    fun `Asserting body should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).body(equalTo("Something else")) }
        assertFails { assertThat(response).body(containsString("Other")) }
        assertFails { assertThat(response).body(isNull()) }
        assertFails {
            assertThat(response)
                .body(equalTo("Something else"))
                .body(containsString("Other"))
                .body(isNull())
        }
    }

    @Test
    fun `Asserting binary body should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).binaryBody(equalTo(response.body?.asBinary())) }
        assertDoesNotThrow { assertThat(response).binaryBody(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .binaryBody(equalTo(expectedBinaryContent.copyOf()))
                .binaryBody(equalTo(expectedBinaryContent))
                .binaryBody(isNotNull())
        }
    }

    @Test
    fun `Asserting binary body should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).binaryBody(equalTo(byteArrayOf(10, 11, 12))) }
        assertFails { assertThat(response).binaryBody(isNull()) }
        assertFails {
            assertThat(response)
                .binaryBody(equalTo(byteArrayOf(10, 11, 12)))
                .binaryBody(isNull())
        }
    }

    @Test
    fun `Asserting base 64 body should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).base64Body(equalTo(response.body?.asBase64())) }
        assertDoesNotThrow { assertThat(response).base64Body(equalTo("eyJuYW1lIjogIkpvaG4iLCAibmVzdGVkIjogeyJrZXkxIjogInZhbHVlMSIsICJrZXkyIjogInZhbHVlMiJ9LCAiZGV0YWlscyI6IFt7ImRldGFpbCI6IDF9LCB7ImRldGFpbCI6IDJ9XX0=")) }
        assertDoesNotThrow { assertThat(response).base64Body(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .base64Body(equalTo(response.body?.asBase64()))
                .base64Body(isNotNull())
        }
    }

    @Test
    fun `Asserting base 64 body should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).base64Body(equalTo("ABC")) }
        assertFails { assertThat(response).base64Body(isNull()) }
        assertFails {
            assertThat(response)
                .base64Body(equalTo("ABC"))
                .base64Body(isNull())
        }
    }

    @Test
    fun `Asserting json body should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("$.name").withValue(equalTo("John"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("$.nested.key1").withValue(equalTo("value1"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("$.nested.key2").withValue(equalTo("value2"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("$.details[0].detail").withValue(equalTo("1"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("$.details[1].detail").withValue(equalTo("2"))) }
        assertDoesNotThrow {
            assertThat(response)
                .jsonBody(containsNode("$.name").withValue(equalTo("John")))
                .jsonBody(containsNode("$.nested.key1").withValue(equalTo("value1")))
                .jsonBody(containsNode("$.nested.key2").withValue(equalTo("value2")))
                .jsonBody(containsNode("$.details[0].detail").withValue(equalTo("1")))
                .jsonBody(containsNode("$.details[1].detail").withValue(equalTo("2")))
        }
    }

    @Test
    fun `Asserting json body equality should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).jsonBody(equalTo(expectedJsonNode)) }
        assertDoesNotThrow { assertThat(response).jsonBody(equalTo(reducedJsonNode).ignoringNodes("$.name")) }
        assertDoesNotThrow { assertThat(response).jsonBody(equalTo(extendedJsonNode).ignoringNodes("$.additional")) }
    }

    @Test
    fun `Asserting json body equality should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).jsonBody(equalTo(reducedJsonNode)) }
        assertFails { assertThat(response).jsonBody(equalTo(extendedJsonNode)) }
    }

    @Test
    fun `Asserting json body should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).jsonBody(containsNode("$.name").withValue(equalTo("Peter"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.nested.key1").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.nested.key2").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.details[0].detail").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.details[1].detail").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.non-existing").withValue(equalTo("Something"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.non-existing")) }
        assertFails { assertThat(response).jsonBody(containsNode("$.details[99].detail").withValue(equalTo("Something"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.name[0]").withValue(equalTo("Peter"))) }
        assertFails { assertThat(response).jsonBody(containsNode("$.details[0]").withValue(equalTo("Something"))) }
        assertFails {
            assertThat(response)
                .jsonBody(containsNode("$.name").withValue(equalTo("Peter")))
                .jsonBody(containsNode("$.nested.key1").withValue(equalTo("other")))
                .jsonBody(containsNode("$.nested.key2").withValue(equalTo("other")))
                .jsonBody(containsNode("$.details[0].detail").withValue(equalTo("other")))
                .jsonBody(containsNode("$.details[1].detail").withValue(equalTo("other")))
                .jsonBody(containsNode("$.non-existing").withValue(equalTo("Something")))
                .jsonBody(containsNode("$.details[99].detail").withValue(equalTo("Something")))
                .jsonBody(containsNode("$.name[0]").withValue(equalTo("Peter")))
                .jsonBody(containsNode("$.details[0]").withValue(equalTo("Something")))
        }
    }

    @Test
    fun `Asserting json body should fail with invalid JSON path`() {
        assertFailsWith<IllegalArgumentException> { assertThat(response).jsonBody(containsNode("$.name[abc]")) }
        assertFailsWith<IllegalArgumentException> { assertThat(response).jsonBody(containsNode("$.invalid-path.")) }
    }

    @Test
    fun `Asserting conformity to specification should not throw if all assertions are satisfied`() {
        val specification = ExpectedResponse()
            .expectStatusCode(equalTo(200))
            .expectStatusCode(isNotNull())
            .expectProtocol(equalTo("http/1.1"))
            .expectProtocol(isNotNull())
            .expectProtocol(containsString("http"))
            .expectMessage(equalTo("Some Message"))
            .expectMessage(containsString("Message"))
            .expectHeaders(containsKey("Key1").withValue(equalTo("Value1.1")))
            .expectHeaders(containsKey("Key1").withValue(equalTo("Value1.2")))
            .expectHeaders(containsKey("Key2").withValue(equalTo("Value2")))
            .expectContentType(equalTo("application/json; charset=utf-8"))
            .expectBody(containsString("name"))
            .expectBase64Body(containsString("eyJuYW1lIjogIkpva"))
            .expectBinaryBody(equalTo(response.body?.asBinary()))
            .expectJsonBody(containsNode("$.name").withValue(equalTo("John")))
            .expectJsonBody(containsNode("$.details[0].detail").withValue(equalTo("1")))
            .expectObjectBody(BodyClass::class.java, equalTo(expectedObj))
            .expectObjectBody(object : TypeReference<BodyClass>() {}, equalTo(expectedObj))
            .expectObjectBody<BodyClass>(equalTo(expectedObj))
            .expectBody(equalToContentsFromFile(filename).asString())
            .expectBase64Body(equalToContentsFromFile(filename).asBase64())
            .expectJsonBody(equalToContentsFromFile(filename).asJson())
            .expectBinaryBody(equalToContentsFromFile(filename).asBinary())
            .expectObjectBody(equalToContentsFromFile(filename).asObject(BodyClass::class.java))
            .expectObjectBody(equalToContentsFromFile(filename).asObject(object : TypeReference<BodyClass>() {}))
            .expectObjectBody<BodyClass>(equalToContentsFromFile(filename).asObject())

        assertDoesNotThrow { assertThat(response).conformsTo(specification) }
    }

    @Test
    fun `Asserting conformity to specification should throw if at least one assertion is not satisfied`() {
        val specification = ExpectedResponse()
            .expectStatusCode(equalTo(400))
            .expectStatusCode(isNull())
            .expectProtocol(equalTo("http/2.0"))
            .expectProtocol(isNull())
            .expectProtocol(containsString("https"))
            .expectMessage(equalTo("Other"))
            .expectMessage(containsString("Something Else"))
            .expectHeaders(containsKey("Key1").withValue(equalTo("Other")))
            .expectHeaders(containsKey("Key3"))
            .expectContentType(equalTo("Other"))
            .expectBody(containsString("Other"))
            .expectBase64Body(containsString("Other"))
            .expectJsonBody(containsNode("$.name").withValue(equalTo("Peter")))
            .expectJsonBody(containsNode("$.details[0]").withValue(equalTo("1")))

        val e = assertFails { assertThat(response).conformsTo(specification) }
        assertEquals(14, e.failures.size)
    }

    @Test
    fun `Asserting body with contents from file should throw if assertions are not satisfied`() {
        val filename = "test-file.txt"
        assertFailsWith<AssertionFailure> { assertThat(response).body(equalToContentsFromFile(filename).asString()) }
        assertFailsWith<AssertionFailure> { assertThat(response).base64Body(equalToContentsFromFile(filename).asBase64()) }
        assertFailsWith<AssertionFailure> { assertThat(response).binaryBody(equalToContentsFromFile(filename).asBinary()) }
        assertFailsWith<ParseException> { assertThat(response).jsonBody(equalToContentsFromFile(filename).asJson()) }
    }

    data class BodyClass(
        val name: String,
        val nested: NestedBodyClass,
        val details: List<DetailsBodyClass>
    )

    data class NestedBodyClass(
        val key1: String,
        val key2: String,
    )

    data class DetailsBodyClass(
        val detail: Int,
    )
}

private fun assertFails(assertion: () -> Unit): AssertionFailure {
    return assertFailsWith<AssertionFailure> { assertion() }
}
