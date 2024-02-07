package org.jholsten.me2e.request.assertions

import org.jholsten.me2e.assertions.*
import org.jholsten.me2e.request.model.*
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

internal class AssertableResponseIT {

    private val response = HttpResponse(
        request = HttpRequest(
            url = Url("https://google.com/"),
            method = HttpMethod.GET,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = null,
        ),
        protocol = "http/1.1",
        message = "Some Message",
        code = 200,
        headers = HttpHeaders(
            mapOf(
                "Key1" to listOf("Value1.1", "Value1.2"),
                "Key2" to listOf("Value2"),
            )
        ),
        body = HttpResponseBody(
            contentType = MediaType.JSON_UTF8,
            content = byteArrayOf(
                123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 74, 111, 104, 110, 34, 44, 32, 34, 110, 101, 115, 116, 101, 100, 34, 58,
                32, 123, 34, 107, 101, 121, 49, 34, 58, 32, 34, 118, 97, 108, 117, 101, 49, 34, 44, 32, 34, 107, 101, 121, 50, 34, 58, 32,
                34, 118, 97, 108, 117, 101, 50, 34, 125, 44, 32, 34, 100, 101, 116, 97, 105, 108, 115, 34, 58, 32, 91, 123, 34, 100, 101,
                116, 97, 105, 108, 34, 58, 32, 49, 125, 44, 32, 123, 34, 100, 101, 116, 97, 105, 108, 34, 58, 32, 50, 125, 93, 125,
            ),
        ),
    )

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
                .jsonBody(containsNode("name").withValue(equalTo("John")))
                .jsonBody(containsNode("nested.key1").withValue(equalTo("value1")))
                .jsonBody(containsNode("nested.key2").withValue(equalTo("value2")))
                .jsonBody(containsNode("details[0].detail").withValue(equalTo("1")))
                .jsonBody(containsNode("details[1].detail").withValue(equalTo("2")))
        }
    }

    @Test
    fun `Asserting status code should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).statusCode(equalTo(response.code)) }
        assertDoesNotThrow { assertThat(response).statusCode(equalTo(200)) }
        assertDoesNotThrow { assertThat(response).statusCode(lessThan(300)) }
        assertDoesNotThrow { assertThat(response).statusCode(greaterThan(100)) }
        assertDoesNotThrow { assertThat(response).statusCode(between(100, 300)) }
        assertDoesNotThrow { assertThat(response).statusCode(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .statusCode(equalTo(response.code))
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
        assertDoesNotThrow { assertThat(response).protocol(contains("http")) }
        assertDoesNotThrow { assertThat(response).protocol(matchesPattern("http.{4}")) }
        assertDoesNotThrow { assertThat(response).protocol(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .protocol(equalTo(response.protocol))
                .protocol(equalTo("http/1.1"))
                .protocol(contains("http"))
                .protocol(matchesPattern("http.{4}"))
                .protocol(isNotNull())
        }
    }

    @Test
    fun `Asserting protocol should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).protocol(equalTo("http/2.0")) }
        assertFails { assertThat(response).protocol(contains("ABC")) }
        assertFails { assertThat(response).protocol(matchesPattern("^[A-Z]{4}\$")) }
        assertFails { assertThat(response).protocol(isNull()) }
        assertFails {
            assertThat(response)
                .protocol(equalTo("http/2.0"))
                .protocol(contains("ABC"))
                .protocol(matchesPattern("^[A-Z]{4}\$"))
                .protocol(isNull())
        }
    }

    @Test
    fun `Asserting message should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).message(equalTo(response.message)) }
        assertDoesNotThrow { assertThat(response).message(equalTo("Some Message")) }
        assertDoesNotThrow { assertThat(response).message(contains("Message")) }
        assertDoesNotThrow { assertThat(response).message(matchesPattern("[\\w|\\s]*")) }
        assertDoesNotThrow { assertThat(response).message(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .message(equalTo(response.message))
                .message(equalTo("Some Message"))
                .message(contains("Message"))
                .message(matchesPattern("[\\w|\\s]*"))
                .message(isNotNull())
        }
    }

    @Test
    fun `Asserting message should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).message(equalTo("Another Message")) }
        assertFails { assertThat(response).message(contains("Something else")) }
        assertFails { assertThat(response).message(matchesPattern("^[A-Z]{4}\$")) }
        assertFails { assertThat(response).message(isNull()) }
        assertFails {
            assertThat(response)
                .message(equalTo("Another Message"))
                .message(contains("Something else"))
                .message(matchesPattern("^[A-Z]{4}\$"))
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
        assertDoesNotThrow { assertThat(response).contentType(contains("application/json")) }
        assertDoesNotThrow { assertThat(response).contentType(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .contentType(equalTo(response.body?.contentType?.value))
                .contentType(equalTo("application/json; charset=utf-8"))
                .contentType(contains("application/json"))
                .contentType(isNotNull())
        }
    }

    @Test
    fun `Asserting content type should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).contentType(equalTo("text/plain; charset=utf-8")) }
        assertFails { assertThat(response).contentType(contains("text/plain")) }
        assertFails { assertThat(response).contentType(isNull()) }
        assertFails {
            assertThat(response)
                .contentType(equalTo("text/plain; charset=utf-8"))
                .contentType(contains("text/plain"))
                .contentType(isNull())
        }
    }

    @Test
    fun `Asserting body should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).body(equalTo(response.body?.asString())) }
        assertDoesNotThrow { assertThat(response).body(equalTo("{\"name\": \"John\", \"nested\": {\"key1\": \"value1\", \"key2\": \"value2\"}, \"details\": [{\"detail\": 1}, {\"detail\": 2}]}")) }
        assertDoesNotThrow { assertThat(response).body(contains("name")) }
        assertDoesNotThrow { assertThat(response).body(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .body(equalTo(response.body?.asString()))
                .body(contains("name"))
                .body(isNotNull())
        }
    }

    @Test
    fun `Asserting body should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).body(equalTo("Something else")) }
        assertFails { assertThat(response).body(contains("Other")) }
        assertFails { assertThat(response).body(isNull()) }
        assertFails {
            assertThat(response)
                .body(equalTo("Something else"))
                .body(contains("Other"))
                .body(isNull())
        }
    }

    @Test
    fun `Asserting binary body should not throw if assertion is satisfied`() {
        assertDoesNotThrow { assertThat(response).binaryBody(equalTo(response.body?.asBinary())) }
        assertDoesNotThrow { assertThat(response).binaryBody(isNotNull()) }
        assertDoesNotThrow {
            assertThat(response)
                .binaryBody(equalTo(response.body?.asBinary()))
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
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("name").withValue(equalTo("John"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("nested.key1").withValue(equalTo("value1"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("nested.key2").withValue(equalTo("value2"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("details[0].detail").withValue(equalTo("1"))) }
        assertDoesNotThrow { assertThat(response).jsonBody(containsNode("details[1].detail").withValue(equalTo("2"))) }
        assertDoesNotThrow {
            assertThat(response)
                .jsonBody(containsNode("name").withValue(equalTo("John")))
                .jsonBody(containsNode("nested.key1").withValue(equalTo("value1")))
                .jsonBody(containsNode("nested.key2").withValue(equalTo("value2")))
                .jsonBody(containsNode("details[0].detail").withValue(equalTo("1")))
                .jsonBody(containsNode("details[1].detail").withValue(equalTo("2")))
        }
    }

    @Test
    fun `Asserting json body should throw if assertion is not satisfied`() {
        assertFails { assertThat(response).jsonBody(containsNode("name").withValue(equalTo("Peter"))) }
        assertFails { assertThat(response).jsonBody(containsNode("nested.key1").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("nested.key2").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("details[0].detail").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("details[1].detail").withValue(equalTo("other"))) }
        assertFails { assertThat(response).jsonBody(containsNode("non-existing").withValue(equalTo("Something"))) }
        assertFails { assertThat(response).jsonBody(containsNode("details[99].detail").withValue(equalTo("Something"))) }
        assertFails { assertThat(response).jsonBody(containsNode("name[0]").withValue(equalTo("Peter"))) }
        assertFails { assertThat(response).jsonBody(containsNode("details[0]").withValue(equalTo("Something"))) }
        assertFails {
            assertThat(response)
                .jsonBody(containsNode("name").withValue(equalTo("Peter")))
                .jsonBody(containsNode("nested.key1").withValue(equalTo("other")))
                .jsonBody(containsNode("nested.key2").withValue(equalTo("other")))
                .jsonBody(containsNode("details[0].detail").withValue(equalTo("other")))
                .jsonBody(containsNode("details[1].detail").withValue(equalTo("other")))
                .jsonBody(containsNode("non-existing").withValue(equalTo("Something")))
                .jsonBody(containsNode("details[99].detail").withValue(equalTo("Something")))
                .jsonBody(containsNode("name[0]").withValue(equalTo("Peter")))
                .jsonBody(containsNode("details[0]").withValue(equalTo("Something")))
        }
    }

    @Test
    fun `Asserting conformity to specification should not throw if all assertions are satisfied`() {
        val specification = ExpectedResponse()
            .expectStatusCode(equalTo(200))
            .expectStatusCode(isNotNull())
            .expectProtocol(equalTo("http/1.1"))
            .expectProtocol(isNotNull())
            .expectProtocol(contains("http"))
            .expectMessage(equalTo("Some Message"))
            .expectMessage(contains("Message"))
            .expectHeaders(containsKey("Key1").withValue(equalTo("Value1.1")))
            .expectHeaders(containsKey("Key1").withValue(equalTo("Value1.2")))
            .expectHeaders(containsKey("Key2").withValue(equalTo("Value2")))
            .expectContentType(equalTo("application/json; charset=utf-8"))
            .expectBody(contains("name"))
            .expectBase64Body(contains("eyJuYW1lIjogIkpva"))
            .expectBinaryBody(equalTo(response.body?.asBinary()))
            .expectJsonBody(containsNode("name").withValue(equalTo("John")))
            .expectJsonBody(containsNode("details[0].detail").withValue(equalTo("1")))

        assertDoesNotThrow { assertThat(response).conformsTo(specification) }
    }

    @Test
    fun `Asserting conformity to specification should throw if at least one assertion is not satisfied`() {
        val specification = ExpectedResponse()
            .expectStatusCode(equalTo(400))
            .expectStatusCode(isNull())
            .expectProtocol(equalTo("http/2.0"))
            .expectProtocol(isNull())
            .expectProtocol(contains("https"))
            .expectMessage(equalTo("Other"))
            .expectMessage(contains("Something Else"))
            .expectHeaders(containsKey("Key1").withValue(equalTo("Other")))
            .expectHeaders(containsKey("Key3"))
            .expectContentType(equalTo("Other"))
            .expectBody(contains("Other"))
            .expectBase64Body(contains("Other"))
            .expectJsonBody(containsNode("name").withValue(equalTo("Peter")))
            .expectJsonBody(containsNode("details[0]").withValue(equalTo("1")))

        val e = assertFails { assertThat(response).conformsTo(specification) }
        assertEquals(14, e.failures.size)
    }
}

private fun assertFails(assertion: () -> Unit): AssertionFailure {
    return assertFailsWith<AssertionFailure> { assertion() }
}
