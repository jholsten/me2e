package org.jholsten.me2e.request.model

import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class HttpRequestBodyTest {

    @Test
    fun `String content should be set in request body`() {
        val body = HttpRequestBody(
            content = "abc",
            contentType = MediaType.TEXT_PLAIN_UTF8,
        )

        assertEquals("abc", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals("YWJj", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `String content with special characters should be set in request body`() {
        val body = HttpRequestBody(
            content = "abcäÄöÖüÜèé%&~'",
            contentType = MediaType.TEXT_PLAIN_UTF8,
        )

        assertEquals("abcäÄöÖüÜèé%&~'", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(
            97, 98, 99, 195.toByte(), 164.toByte(), 195.toByte(), 132.toByte(), 195.toByte(), 182.toByte(),
            195.toByte(), 150.toByte(), 195.toByte(), 188.toByte(), 195.toByte(), 156.toByte(), 195.toByte(),
            168.toByte(), 195.toByte(), 169.toByte(), 37, 38, 126, 39
        ), body.asBinary())
        assertEquals("YWJjw6TDhMO2w5bDvMOcw6jDqSUmfic=", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `File content should be set in request body`() {
        val file = Files.createTempFile("test", ".tmp")
        Files.write(file, "abc".toByteArray())
        val body = HttpRequestBody(
            content = file.toFile(),
            contentType = MediaType.TEXT_PLAIN_UTF8,
        )

        assertEquals("abc", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals("YWJj", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Binary content should be set in request body`() {
        val body = HttpRequestBody(
            content = byteArrayOf(97, 98, 99),
            contentType = MediaType.TEXT_PLAIN_UTF8,
        )

        assertEquals("abc", body.asString())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals("YWJj", body.asBase64())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }
}
