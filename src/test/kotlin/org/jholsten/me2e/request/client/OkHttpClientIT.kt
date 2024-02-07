package org.jholsten.me2e.request.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.awaitility.Awaitility
import org.awaitility.Durations
import org.jholsten.me2e.assertions.assertThat
import org.jholsten.me2e.assertions.containsKey
import org.jholsten.me2e.assertions.equalTo
import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OkHttpClientIT {

    private val wireMockServer = WireMockServer()

    @BeforeAll
    fun beforeAll() {
        wireMockServer.start()
        Awaitility.await().atMost(Durations.FIVE_SECONDS).until { wireMockServer.isRunning }
    }

    @AfterAll
    fun afterAll() {
        wireMockServer.stop()
    }

    @BeforeTest
    fun beforeTest() {
        wireMockServer.stubFor(
            WireMock.any(WireMock.anyUrl())
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withBody("Some Response")
                        .withHeader("Content-Type", "text/plain")
                )
        )
    }

    @AfterTest
    fun afterTest() {
        wireMockServer.resetAll()
    }

    @ParameterizedTest
    @EnumSource(HttpMethod::class, names = ["GET", "DELETE", "OPTIONS"])
    fun `Executing request without body should succeed`(httpMethod: HttpMethod) {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url(wireMockServer.baseUrl()))
            .build()

        val headers = HttpHeaders(mapOf("Name" to listOf("Value")))
        val url = Url(wireMockServer.baseUrl()).withRelativeUrl(
            RelativeUrl.Builder().withPath("/search").withQueryParameter("id", "123").build()
        )
        val request = HttpRequest.Builder()
            .withUrl(url)
            .withMethod(httpMethod)
            .withHeaders(headers)
            .build()
        val response = httpClient.execute(request)

        assertResponseAsExpected(response)
        assertRequestWasSent(
            url = wireMockServer.url("/search?id=123"),
            method = httpMethod,
            headers = headers,
            body = null,
        )
    }

    @ParameterizedTest
    @EnumSource(HttpMethod::class, names = ["PUT", "POST", "PATCH", "DELETE"])
    fun `Executing request with body should succeed`(httpMethod: HttpMethod) {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url(wireMockServer.baseUrl()))
            .build()


        val headers = HttpHeaders(mapOf("Name" to listOf("Value")))
        val url = Url(wireMockServer.baseUrl()).withRelativeUrl(
            RelativeUrl.Builder().withPath("/search").withQueryParameter("id", "123").build()
        )
        val body = HttpRequestBody("Some Request", MediaType.TEXT_PLAIN_UTF8)
        val request = HttpRequest.Builder()
            .withUrl(url)
            .withMethod(httpMethod)
            .withHeaders(headers)
            .withBody(body)
            .build()

        val response = httpClient.execute(request)

        assertResponseAsExpected(response)
        assertRequestWasSent(
            url = wireMockServer.url("/search?id=123"),
            method = httpMethod,
            headers = headers,
            body = body,
        )
    }

    @Test
    fun `Executing request with interceptor should call interceptor`() {
        val interceptor = object : RequestInterceptor {
            override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                val request = chain.getRequest().newBuilder()
                    .addHeader("Authorization", "Bearer 123")
                    .build()
                return chain.proceed(request)
            }
        }

        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url(wireMockServer.baseUrl()))
            .withRequestInterceptors(listOf(interceptor))
            .build()


        val headers = HttpHeaders(mapOf("Name" to listOf("Value")))
        val url = Url(wireMockServer.baseUrl()).withRelativeUrl(
            RelativeUrl.Builder().withPath("/search").withQueryParameter("id", "123").build()
        )
        val request = HttpRequest.Builder()
            .withUrl(url)
            .withMethod(HttpMethod.GET)
            .withHeaders(headers)
            .build()

        val response = httpClient.execute(request)

        assertResponseAsExpected(response)
        assertRequestWasSent(
            url = wireMockServer.url("/search?id=123"),
            method = HttpMethod.GET,
            headers = headers.newBuilder().add("Authorization", "Bearer 123").build(),
            body = null,
        )
    }

    private fun assertResponseAsExpected(response: HttpResponse) {
        assertEquals(200, response.code)
        assertNotNull(response.body)
        assertEquals("Some Response", response.body!!.asString())
        assertTrue("Content-Type" in response.headers)
        assertEquals(listOf("text/plain"), response.headers["Content-Type"])
        assertThat(response).statusCode(equalTo(200))
        assertThat(response).body(equalTo("Some Response"))
        assertThat(response).contentType(equalTo("text/plain"))
        assertThat(response).headers(containsKey("Content-Type").withValue(equalTo("text/plain")))
    }

    private fun assertRequestWasSent(url: String, method: HttpMethod, headers: HttpHeaders, body: HttpRequestBody?) {
        assertEquals(1, wireMockServer.allServeEvents.size)
        val request = wireMockServer.allServeEvents.first().request
        assertEquals(url, request.absoluteUrl)
        assertEquals(method, HttpMethod.valueOf(request.method.value()))
        for ((header, values) in headers) {
            assertEquals(values, request.headers.getHeader(header).values())
        }
        if (body == null) {
            assertTrue(request.body == null || request.body.isEmpty())
        } else {
            assertTrue(request.body != null && request.body.isNotEmpty())
            assertEquals(body.asString(), request.bodyAsString)
        }
    }
}
