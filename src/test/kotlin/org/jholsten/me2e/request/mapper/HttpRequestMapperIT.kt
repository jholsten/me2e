package org.jholsten.me2e.request.mapper

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.MediaType
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.Test

internal class HttpRequestMapperIT {

    @Test
    fun `Mapping okhttp3 GET Request should succeed`() {
        val result = HttpRequestMapper.INSTANCE.toInternalDto(Request.Builder()
            .get().url("https://google.com")
            .header("Name", "Value")
            .build())

        val expected = HttpRequest(
            url = "https://google.com/",
            method = HttpMethod.GET,
            headers = mapOf("Name" to listOf("Value")),
            body = null,
        )

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping okhttp3 POST Request with body should succeed`() {
        val body = "{\"name\": \"value\"}".toRequestBody("application/json".toMediaType())
        val result = HttpRequestMapper.INSTANCE.toInternalDto(Request.Builder()
            .post(body).url("https://google.com")
            .header("Name", "Value")
            .build())

        val expected = HttpRequest(
            url = "https://google.com/",
            method = HttpMethod.POST,
            headers = mapOf("Name" to listOf("Value")),
            body = HttpRequestBody(
                content = "{\"name\": \"value\"}",
                contentType = MediaType("application/json"),
            ),
        )

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping GET Request to okhttp should succeed`() {
        val result = HttpRequestMapper.INSTANCE.toOkHttpRequest(HttpRequest(
            url = "https://google.com/",
            method = HttpMethod.GET,
            headers = mapOf("Name" to listOf("Value")),
            body = null,
        ))

        val expected = Request.Builder()
            .get().url("https://google.com")
            .header("Name", "Value")
            .build()

        RecursiveComparison.assertEquals(expected, result)
    }

    @Test
    fun `Mapping POST Request with body to okhttp should succeed`() {
        val result = HttpRequestMapper.INSTANCE.toOkHttpRequest(HttpRequest(
            url = "https://google.com/",
            method = HttpMethod.POST,
            headers = mapOf("Name" to listOf("Value")),
            body = HttpRequestBody(
                content = "{\"name\": \"value\"}",
                contentType = MediaType("application/json"),
            ),
        ))

        val expected = Request.Builder()
            .post("{\"name\": \"value\"}".toRequestBody("application/json".toMediaType())).url("https://google.com")
            .header("Name", "Value")
            .build()

        RecursiveComparison.assertEquals(expected, result)
    }
}
