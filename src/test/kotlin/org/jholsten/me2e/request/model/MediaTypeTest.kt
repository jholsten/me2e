package org.jholsten.me2e.request.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class MediaTypeTest {

    @Test
    fun `Constructor should initialize type and subtype`() {
        val mediaType = MediaType("application/json")

        assertEquals("application", mediaType.type)
        assertEquals("json", mediaType.subtype)
    }

    @Test
    fun `Constructor with parameters should initialize type and subtype`() {
        val mediaType = MediaType("application/json; charset=utf-8; version=1")

        assertEquals("application", mediaType.type)
        assertEquals("json", mediaType.subtype)
    }

    @Test
    fun `Constructor with invalid format should throw exception`() {
        val e = assertThrowsExactly(IllegalArgumentException::class.java) { MediaType("invalid") }
        assertEquals("Invalid media type \"invalid\"", e.message)
    }

    @Test
    fun `Media type without parameters should not contain parameters`() {
        val mediaType = MediaType("application/json; charset=utf-8; version=1")

        assertEquals("application/json", mediaType.withoutParameters())
    }

    @ParameterizedTest(name = "[{index}] String Media Type {0} should be interpreted as String")
    @ValueSource(strings = ["text/plain", "text/*", "application/json", "application/xml"])
    fun `String media types should be interpreted as Strings`(value: String) {
        val mediaType = MediaType(value)

        assertTrue(mediaType.isStringType())
    }

    @ParameterizedTest(name = "[{index}] Binary Media Type {0} should not be interpreted as String")
    @ValueSource(strings = ["application/pdf", "image/png", "video/mp4", "application/xhtml+xml"])
    fun `Binary media types should not be interpreted as Strings`(value: String) {
        val mediaType = MediaType(value)

        assertFalse(mediaType.isStringType())
    }
}
