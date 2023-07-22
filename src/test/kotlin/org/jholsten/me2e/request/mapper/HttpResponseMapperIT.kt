package org.jholsten.me2e.request.mapper

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jholsten.me2e.request.model.*
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.Test

internal class HttpResponseMapperIT {
    @Test
    fun `Mapping okhttp3 response should succeed`() {
        val result = HttpResponseMapper.INSTANCE.toInternalDto(Response.Builder()
            .request(Request.Builder()
                .get().url("https://google.com")
                .header("Name", "Value")
                .build())
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .header("Authorization", "Bearer 123")
            .build()
        )

        val expected = HttpResponse(
            request = HttpRequest(
                url = "https://google.com/",
                method = HttpMethod.GET,
                headers = mutableMapOf("Name" to listOf("Value")),
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            headers = mapOf("Authorization" to listOf("Bearer 123")),
            body = null,
        )

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping okhttp3 response with body should succeed`() {
        val body = "{\"name\": \"value\"}".toResponseBody("application/json".toMediaType())
        val result = HttpResponseMapper.INSTANCE.toInternalDto(Response.Builder()
            .request(Request.Builder()
                .get().url("https://google.com")
                .header("Name", "Value")
                .build())
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .header("Authorization", "Bearer 123")
            .body(body)
            .build()
        )

        val expected = HttpResponse(
            request = HttpRequest(
                url = "https://google.com/",
                method = HttpMethod.GET,
                headers = mutableMapOf("Name" to listOf("Value")),
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            headers = mapOf("Authorization" to listOf("Bearer 123")),
            body = HttpResponseBody(
                contentType = MediaType("application/json"),
                contentLength = 17,
                stringContent = "{\"name\": \"value\"}",
                binaryContent = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
            ),
        )

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping okhttp3 response with body with special characters should succeed`() {
        val body = "abcäÄöÖüÜèé%&~'".toResponseBody("text/plain".toMediaType())
        val result = HttpResponseMapper.INSTANCE.toInternalDto(Response.Builder()
            .request(Request.Builder()
                .get().url("https://google.com")
                .build())
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .body(body)
            .build()
        )

        val expected = HttpResponse(
            request = HttpRequest(
                url = "https://google.com/",
                method = HttpMethod.GET,
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            body = HttpResponseBody(
                contentType = MediaType("text/plain"),
                contentLength = 23,
                stringContent = "abcäÄöÖüÜèé%&~'",
                binaryContent = byteArrayOf(
                    97, 98, 99, 195.toByte(), 164.toByte(), 195.toByte(), 132.toByte(), 195.toByte(), 182.toByte(),
                    195.toByte(), 150.toByte(), 195.toByte(), 188.toByte(), 195.toByte(), 156.toByte(), 195.toByte(),
                    168.toByte(), 195.toByte(), 169.toByte(), 37, 38, 126, 39
                ),
            ),
        )

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping response to okhttp3 response should succeed`() {
        val result = HttpResponseMapper.INSTANCE.toOkHttpResponse(HttpResponse(
            request = HttpRequest(
                url = "https://google.com/",
                method = HttpMethod.GET,
                headers = mutableMapOf("Name" to listOf("Value")),
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            headers = mapOf("Authorization" to listOf("Bearer 123")),
            body = null,
        ))

        val expected = Response.Builder()
            .request(Request.Builder()
                .get().url("https://google.com")
                .header("Name", "Value")
                .build())
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .header("Authorization", "Bearer 123")
            .build()

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping response to okhttp3 response with body should succeed`() {
        val result = HttpResponseMapper.INSTANCE.toOkHttpResponse(HttpResponse(
            request = HttpRequest(
                url = "https://google.com/",
                method = HttpMethod.GET,
                headers = mutableMapOf("Name" to listOf("Value")),
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            headers = mapOf("Authorization" to listOf("Bearer 123")),
            body = HttpResponseBody(
                contentType = MediaType("application/json"),
                contentLength = 17,
                stringContent = "{\"name\": \"value\"}",
                binaryContent = byteArrayOf(123, 34, 110, 97, 109, 101, 34, 58, 32, 34, 118, 97, 108, 117, 101, 34, 125),
            ),
        ))

        val expected = Response.Builder()
            .request(Request.Builder()
                .get().url("https://google.com")
                .header("Name", "Value")
                .build())
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .header("Authorization", "Bearer 123")
            .body("{\"name\": \"value\"}".toResponseBody("application/json".toMediaType()))
            .build()

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping response to okhttp3 response with body with special characters should succeed`() {
        val result = HttpResponseMapper.INSTANCE.toOkHttpResponse(HttpResponse(
            request = HttpRequest(
                url = "https://google.com/",
                method = HttpMethod.GET,
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            body = HttpResponseBody(
                contentType = MediaType("text/plain"),
                contentLength = 23,
                stringContent = "abcäÄöÖüÜèé%&~'",
                binaryContent = byteArrayOf(
                    97, 98, 99, 195.toByte(), 164.toByte(), 195.toByte(), 132.toByte(), 195.toByte(), 182.toByte(),
                    195.toByte(), 150.toByte(), 195.toByte(), 188.toByte(), 195.toByte(), 156.toByte(), 195.toByte(),
                    168.toByte(), 195.toByte(), 169.toByte(), 37, 38, 126, 39
                ),
            ),
        ))

        val expected = Response.Builder()
            .request(Request.Builder()
                .get().url("https://google.com")
                .build())
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .body("abcäÄöÖüÜèé%&~'".toResponseBody("text/plain".toMediaType()))
            .build()

        RecursiveComparison.assertEquals(expected, result)
    }
}
