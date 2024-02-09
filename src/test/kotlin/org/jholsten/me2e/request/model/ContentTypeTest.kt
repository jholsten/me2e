package org.jholsten.me2e.request.model

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.*

internal class ContentTypeTest {

    @Test
    fun `Constructor should initialize type and subtype`() {
        val contentType = ContentType("application/json")

        assertEquals("application", contentType.type)
        assertEquals("json", contentType.subtype)
    }

    @Test
    fun `Constructor with parameters should initialize type and subtype`() {
        val contentType = ContentType("application/json; charset=utf-8; version=1")

        assertEquals("application", contentType.type)
        assertEquals("json", contentType.subtype)
    }

    @Test
    fun `Constructor with invalid format should throw exception`() {
        val e = assertFailsWith<IllegalArgumentException> { ContentType("invalid") }
        assertEquals("Invalid content type \"invalid\"", e.message)
    }

    @Test
    fun `Content type without parameters should not contain parameters`() {
        val contentType = ContentType("application/json; charset=utf-8; version=1")

        assertEquals("application/json", contentType.withoutParameters())
    }

    @ParameterizedTest(name = "[{index}] String Content type {0} should be interpreted as String")
    @ValueSource(strings = ["text/plain", "text/*", "application/json", "application/xml"])
    fun `String content types should be interpreted as Strings`(value: String) {
        val contentType = ContentType(value)

        assertTrue(contentType.isStringType())
    }

    @ParameterizedTest(name = "[{index}] Binary Content type {0} should not be interpreted as String")
    @ValueSource(strings = ["application/pdf", "image/png", "video/mp4", "application/xhtml+xml"])
    fun `Binary content types should not be interpreted as Strings`(value: String) {
        val contentType = ContentType(value)

        assertFalse(contentType.isStringType())
    }
}
