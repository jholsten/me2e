package org.jholsten.me2e.request.model

import org.jholsten.util.RecursiveComparison
import kotlin.test.*

internal class HttpResponseBodyTest {

    @Test
    fun `Content should be decoded as string`() {
        val body = HttpResponseBody(
            contentType = MediaType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
        )

        assertEquals("{\"name\": \"value\"}", body.asString())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content with special characters should be decoded as string`() {
        val body = HttpResponseBody(
            contentType = MediaType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(
                97, 98, 99, 195.toByte(), 164.toByte(), 195.toByte(), 132.toByte(), 195.toByte(), 182.toByte(),
                195.toByte(), 150.toByte(), 195.toByte(), 188.toByte(), 195.toByte(), 156.toByte(), 195.toByte(),
                168.toByte(), 195.toByte(), 169.toByte(), 37, 38, 126, 39
            ),
        )

        assertEquals("abcäÄöÖüÜèé%&~'", body.asString())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content should be returned as byte array`() {
        val body = HttpResponseBody(
            contentType = MediaType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(123, 34),
        )

        RecursiveComparison.assertEquals(byteArrayOf(123, 34), body.asBinary())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Content should be encoded as Base 64`() {
        val body = HttpResponseBody(
            contentType = MediaType.TEXT_PLAIN_UTF8,
            content = byteArrayOf(97, 98, 99),
        )

        assertEquals("abc", body.asString())
        assertEquals("YWJj", body.asBase64())
        RecursiveComparison.assertEquals(byteArrayOf(97, 98, 99), body.asBinary())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }

    @Test
    fun `Empty content should be returned as null`() {
        val body = HttpResponseBody(
            contentType = MediaType.TEXT_PLAIN_UTF8,
            content = null,
        )

        assertNull(body.asString())
        assertNull(body.asBase64())
        assertNull(body.asBinary())
        assertEquals(MediaType.TEXT_PLAIN_UTF8, body.contentType)
    }
}
