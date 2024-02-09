package org.jholsten.me2e.request.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.util.RecursiveComparison
import kotlin.test.*


internal class HttpResponseBodyTest {

    @Test
    fun `Content should be decoded as string`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
        )

        assertEquals("{\"name\": \"value\"}", body.asString())
        assertEquals(ContentType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content with special characters should be decoded as string`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(
                97, 98, 99, 195.toByte(), 164.toByte(), 195.toByte(), 132.toByte(), 195.toByte(), 182.toByte(),
                195.toByte(), 150.toByte(), 195.toByte(), 188.toByte(), 195.toByte(), 156.toByte(), 195.toByte(),
                168.toByte(), 195.toByte(), 169.toByte(), 37, 38, 126, 39
            ),
        )

        assertEquals("abcäÄöÖüÜèé%&~'", body.asString())
        assertEquals(ContentType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content with single quote should be decoded as string with quote`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(
                34, 79, 75
            ),
        )

        assertEquals("\"OK", body.asString())
        assertEquals(ContentType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content should be returned as byte array`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(123, 34),
        )

        RecursiveComparison.assertEquals(byteArrayOf(123, 34), body.asBinary())
        assertEquals(ContentType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content should be encoded as Base 64`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(97, 98, 99),
        )

        assertEquals("abc", body.asString())
        assertEquals("YWJj", body.asBase64())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals(ContentType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content should be decoded as JSON`() {
        val body = HttpResponseBody(
            contentType = ContentType.JSON_UTF8,
            content = byteArrayOf(
                123, 34, 102, 105, 114, 115, 116, 34, 58, 32, 34, 118, 97, 108, 117, 101, 49, 34, 44, 32, 34, 115, 101, 99, 111, 110, 100,
                34, 58, 32, 34, 118, 97, 108, 117, 101, 50, 34, 125
            ),
        )

        assertEquals("{\"first\": \"value1\", \"second\": \"value2\"}", body.asString())
        assertEquals(JsonNodeFactory.instance.objectNode().put("first", "value1").put("second", "value2"), body.asJson())
    }

    @Test
    fun `Decoding invalid content as JSON should throw`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(97, 98, 99),
        )

        assertEquals("abc", body.asString())
        assertFailsWith<ParseException> { body.asJson() }
    }

    @Test
    fun `Content should be decoded as Object`() {
        val body = HttpResponseBody(
            contentType = ContentType.JSON_UTF8,
            content = byteArrayOf(
                123, 34, 102, 105, 114, 115, 116, 34, 58, 32, 34, 118, 97, 108, 117, 101, 49, 34, 44, 32, 34, 115, 101, 99, 111, 110, 100,
                34, 58, 32, 34, 118, 97, 108, 117, 101, 50, 34, 125
            ),
        )

        assertEquals("{\"first\": \"value1\", \"second\": \"value2\"}", body.asString())
        assertEquals(BodyClass("value1", "value2"), body.asObject(BodyClass::class.java))
        assertEquals(BodyClass("value1", "value2"), body.asObject<BodyClass>())
        assertEquals(BodyClass("value1", "value2"), body.asObject(object : TypeReference<BodyClass>() {}))
    }

    @Test
    fun `Decoding invalid content as Object should throw`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(97, 98, 99),
        )

        assertEquals("abc", body.asString())
        assertFailsWith<ParseException> { body.asObject(BodyClass::class.java) }
        assertFailsWith<ParseException> { body.asObject<BodyClass>() }
        assertFailsWith<ParseException> { body.asObject(object : TypeReference<BodyClass>() {}) }
    }

    @Test
    fun `Decoding non-deserializable content as Object should throw`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
        )

        assertEquals("{\"name\": \"value\"}", body.asString())
        assertFailsWith<ParseException> { body.asObject(BodyClass::class.java) }
        assertFailsWith<ParseException> { body.asObject<BodyClass>() }
        assertFailsWith<ParseException> { body.asObject(object : TypeReference<BodyClass>() {}) }
    }

    @Test
    fun `Content should be decoded as Object with generic type`() {
        val body = HttpResponseBody(
            contentType = ContentType.JSON_UTF8,
            content = byteArrayOf(
                123, 34, 102, 105, 114, 115, 116, 34, 58, 32, 34, 118, 97, 108, 117, 101, 49, 34, 44, 32, 34, 115, 101, 99, 111, 110, 100,
                34, 58, 32, 34, 118, 97, 108, 117, 101, 50, 34, 125
            ),
        )

        assertEquals("{\"first\": \"value1\", \"second\": \"value2\"}", body.asString())
        assertEquals(Pair("value1", "value2"), body.asObject(object : TypeReference<Pair<String, String>>() {}))
        assertEquals(Pair("value1", "value2"), body.asObject<Pair<String, String>>())
    }

    @Test
    fun `Null content should be returned as null`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = null,
        )

        assertNull(body.asString())
        assertNull(body.asBase64())
        assertNull(body.asBinary())
        assertNull(body.asJson())
        assertNull(body.asObject(Object::class.java))
        assertEquals(ContentType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Empty content should be returned as empty`() {
        val body = HttpResponseBody(
            contentType = ContentType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(),
        )

        RecursiveComparison.assertEquals(byteArrayOf(), body.asBinary())
        assertEquals("", body.asString())
        assertEquals("", body.asBase64())
        assertFailsWith<ParseException> { body.asObject<Any>() }
        val json = body.asJson()
        assertNotNull(json)
        assertTrue(json.isMissingNode)
        assertTrue(json.isEmpty)
    }

    data class BodyClass(val first: String, val second: String)
}
