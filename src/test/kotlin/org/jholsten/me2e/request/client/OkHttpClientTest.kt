package org.jholsten.me2e.request.client

import io.mockk.*
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jholsten.me2e.request.interceptor.OkHttpRequestInterceptor
import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.mapper.HttpResponseMapper
import org.jholsten.me2e.request.model.*
import org.jholsten.util.RecursiveComparison
import java.util.concurrent.TimeUnit
import kotlin.test.*

internal class OkHttpClientTest {

    private val okHttpClient = mockk<okhttp3.OkHttpClient>()
    private val httpRequestMapper = mockk<HttpRequestMapper>()
    private val httpResponseMapper = mockk<HttpResponseMapper>()
    private val call = mockk<Call>()

    @BeforeTest
    fun beforeTest() {
        mockOkHttpClientBuilder()
        mockkObject(OkHttpRequestInterceptor.Companion)

        mockkObject(HttpRequestMapper.Companion)
        every { HttpRequestMapper.INSTANCE } returns httpRequestMapper

        mockkObject(HttpResponseMapper.Companion)
        every { HttpResponseMapper.INSTANCE } returns httpResponseMapper
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Configuration should be applied to okhttp3 client`() {
        val interceptors = listOf(
            object : RequestInterceptor {
                override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                    val request = chain.getRequest().newBuilder()
                        .addHeader("Authorization", "Bearer 123")
                        .build()
                    return chain.proceed(request)
                }
            },
            object : RequestInterceptor {
                override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                    return chain.proceed(chain.getRequest())
                }
            }
        )
        val mappedInterceptor = object : OkHttpRequestInterceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request())
            }
        }
        every { OkHttpRequestInterceptor.fromRequestInterceptor(any()) } returns mappedInterceptor

        val configuration = OkHttpClient.Configuration()

        configuration.setConnectTimeout(5, TimeUnit.MILLISECONDS)
        configuration.setReadTimeout(6, TimeUnit.MILLISECONDS)
        configuration.setWriteTimeout(7, TimeUnit.MILLISECONDS)
        for (interceptor in interceptors) {
            configuration.addRequestInterceptor(interceptor)
        }
        configuration.apply()

        assertEquals(interceptors, configuration.requestInterceptors)
        assertEquals(5, configuration.connectTimeout)
        assertEquals(6, configuration.readTimeout)
        assertEquals(7, configuration.writeTimeout)
        assertEquals(okHttpClient, configuration.httpClient)
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().connectTimeout(5, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().readTimeout(6, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().writeTimeout(7, TimeUnit.MILLISECONDS) }
        verify(exactly = interceptors.size) { anyConstructed<okhttp3.OkHttpClient.Builder>().addInterceptor(mappedInterceptor) }
        for (interceptor in interceptors) {
            verify { OkHttpRequestInterceptor.fromRequestInterceptor(interceptor) }
        }
    }

    @Test
    fun `Configuration with default values should be applied to okhttp3 client`() {
        val configuration = OkHttpClient.Configuration()
        configuration.apply()

        assertEquals(0, configuration.requestInterceptors.size)
        assertEquals(10000, configuration.connectTimeout)
        assertEquals(10000, configuration.readTimeout)
        assertEquals(10000, configuration.writeTimeout)
        assertEquals(okHttpClient, configuration.httpClient)
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().connectTimeout(10000, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().readTimeout(10000, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().writeTimeout(10000, TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `Configuration with negative connect timeout should fail`() {
        val configuration = OkHttpClient.Configuration()
        assertFailsWith<IllegalArgumentException> {
            configuration.setConnectTimeout(-1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Configuration with connect timeout too large should fail`() {
        val configuration = OkHttpClient.Configuration()
        assertFailsWith<IllegalArgumentException> {
            configuration.setConnectTimeout(Integer.MAX_VALUE.toLong() + 1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Configuration with negative read timeout should fail`() {
        val configuration = OkHttpClient.Configuration()
        assertFailsWith<IllegalArgumentException> {
            configuration.setReadTimeout(-1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Configuration with read timeout too large should fail`() {
        val configuration = OkHttpClient.Configuration()
        assertFailsWith<IllegalArgumentException> {
            configuration.setReadTimeout(Integer.MAX_VALUE.toLong() + 1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Configuration with negative write timeout should fail`() {
        val configuration = OkHttpClient.Configuration()
        assertFailsWith<IllegalArgumentException> {
            configuration.setWriteTimeout(-1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Configuration with write timeout too large should fail`() {
        val configuration = OkHttpClient.Configuration()
        assertFailsWith<IllegalArgumentException> {
            configuration.setWriteTimeout(Integer.MAX_VALUE.toLong() + 1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `Builder should set correct configuration`() {
        val interceptors = listOf(
            object : RequestInterceptor {
                override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                    val request = chain.getRequest().newBuilder()
                        .addHeader("Authorization", "Bearer 123")
                        .build()
                    return chain.proceed(request)
                }
            },
            object : RequestInterceptor {
                override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                    return chain.proceed(chain.getRequest())
                }
            }
        )
        val mappedInterceptor = object : OkHttpRequestInterceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request())
            }
        }
        every { OkHttpRequestInterceptor.fromRequestInterceptor(any()) } returns mappedInterceptor

        val builder = OkHttpClient.Builder()
            .withBaseUrl(Url("https://google.com/"))
            .withConnectTimeout(5, TimeUnit.MILLISECONDS)
            .withReadTimeout(6, TimeUnit.MILLISECONDS)
            .withWriteTimeout(7, TimeUnit.MILLISECONDS)
        for (interceptor in interceptors) {
            builder.addRequestInterceptor(interceptor)
        }
        val httpClient = builder.build()

        assertEquals("https://google.com/", httpClient.baseUrl.value)
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().connectTimeout(5, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().readTimeout(6, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().writeTimeout(7, TimeUnit.MILLISECONDS) }
        verify(exactly = interceptors.size) { anyConstructed<okhttp3.OkHttpClient.Builder>().addInterceptor(mappedInterceptor) }
        for (interceptor in interceptors) {
            verify { OkHttpRequestInterceptor.fromRequestInterceptor(interceptor) }
        }
    }

    @Test
    fun `Builder without base URL should fail`() {
        assertFailsWith<IllegalArgumentException> {
            OkHttpClient.Builder().build()
        }
    }

    @Test
    fun `Builder with default values should set correct configuration`() {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url("https://google.com/"))
            .build()

        assertEquals("https://google.com/", httpClient.baseUrl.value)
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().connectTimeout(10000, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().readTimeout(10000, TimeUnit.MILLISECONDS) }
        verify { anyConstructed<okhttp3.OkHttpClient.Builder>().writeTimeout(10000, TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `Setting request interceptors for existing instance should change okhttp3 configuration`() {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url("https://google.com/"))
            .build()

        val interceptors = listOf(
            object : RequestInterceptor {
                override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                    val request = chain.getRequest().newBuilder()
                        .addHeader("Authorization", "Bearer 123")
                        .build()
                    return chain.proceed(request)
                }
            },
            object : RequestInterceptor {
                override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                    return chain.proceed(chain.getRequest())
                }
            }
        )
        val mappedInterceptor = object : OkHttpRequestInterceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                return chain.proceed(chain.request())
            }
        }
        every { OkHttpRequestInterceptor.fromRequestInterceptor(any()) } returns mappedInterceptor

        httpClient.setRequestInterceptors(interceptors)

        assertEquals("https://google.com/", httpClient.baseUrl.value)
        for (interceptor in interceptors) {
            verify { OkHttpRequestInterceptor.fromRequestInterceptor(interceptor) }
        }
    }

    @Test
    fun `Executing GET request should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/search?name=dog&id=1"),
            method = HttpMethod.GET,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = null,
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.get(
                RelativeUrl.Builder()
                    .withPath("/search")
                    .withQueryParameter("name", "dog")
                    .withQueryParameter("id", "1")
                    .build(),
                headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            )
        }
    }

    @Test
    fun `Executing GET request without required arguments should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/search"),
            method = HttpMethod.GET,
            headers = HttpHeaders.empty(),
            body = null,
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.get(RelativeUrl.Builder().withPath("/search").build())
        }
    }

    @Test
    fun `Executing POST request should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/upload?name=dog&id=1"),
            method = HttpMethod.POST,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.post(
                RelativeUrl.Builder()
                    .withPath("/upload")
                    .withQueryParameter("name", "dog")
                    .withQueryParameter("id", "1")
                    .build(),
                headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing POST request without required arguments should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/upload"),
            method = HttpMethod.POST,
            headers = HttpHeaders.empty(),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.post(
                RelativeUrl.Builder().withPath("/upload").build(),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing PUT request should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/upload?name=dog&id=1"),
            method = HttpMethod.PUT,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.put(
                RelativeUrl.Builder()
                    .withPath("/upload")
                    .withQueryParameter("name", "dog")
                    .withQueryParameter("id", "1")
                    .build(),
                headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing PUT request without required arguments should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/upload"),
            method = HttpMethod.PUT,
            headers = HttpHeaders.empty(),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.put(
                RelativeUrl.Builder().withPath("/upload").build(),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing PATCH request should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/upload?name=dog&id=1"),
            method = HttpMethod.PATCH,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.patch(
                RelativeUrl.Builder()
                    .withPath("/upload")
                    .withQueryParameter("name", "dog")
                    .withQueryParameter("id", "1")
                    .build(),
                headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing PATCH request without required arguments should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/upload"),
            method = HttpMethod.PATCH,
            headers = HttpHeaders.empty(),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.patch(
                RelativeUrl.Builder().withPath("/upload").build(),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing DELETE request should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/image?name=dog&id=1"),
            method = HttpMethod.DELETE,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.delete(
                RelativeUrl.Builder()
                    .withPath("/image")
                    .withQueryParameter("name", "dog")
                    .withQueryParameter("id", "1")
                    .build(),
                headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
                body = HttpRequestBody("Hello World", MediaType.TEXT_PLAIN_UTF8),
            )
        }
    }

    @Test
    fun `Executing DELETE request without required arguments should succeed`() {
        val httpRequest = HttpRequest(
            url = Url("https://google.com/image"),
            method = HttpMethod.DELETE,
            headers = HttpHeaders.empty(),
            body = null,
        )

        executeRequestTest(httpRequest) { httpClient ->
            httpClient.delete(RelativeUrl.Builder().withPath("/image").build())
        }
    }

    @Test
    fun `Executing request without response body should succeed`() {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url("https://google.com/"))
            .build()

        val httpRequest = HttpRequest(
            url = Url("https://google.com/search?name=dog&id=1"),
            method = HttpMethod.GET,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = null,
        )
        val okHttpRequest = okHttpRequestWithoutBody()
        val okHttpResponse = okHttpResponseWithoutBody()
        val httpResponse = httpResponse()

        every { httpRequestMapper.toOkHttpRequest(any()) } returns okHttpRequest
        every { httpResponseMapper.toInternalDto(any()) } returns httpResponse
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns okHttpResponse

        val response = httpClient.execute(httpRequest)

        assertEquals(httpResponse, response)
        verify {
            httpRequestMapper.toOkHttpRequest(match {
                RecursiveComparison.isEqualTo(httpRequest, it)
            })
        }
        verify { httpResponseMapper.toInternalDto(okHttpResponse) }
        verify { okHttpClient.newCall(okHttpRequest) }
        verify { call.execute() }
    }

    @Test
    fun `Executing request with response body should succeed`() {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url("https://google.com/"))
            .build()

        val httpRequest = HttpRequest(
            url = Url("https://google.com/search?name=dog&id=1"),
            method = HttpMethod.GET,
            headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
            body = null,
        )
        val okHttpRequest = okHttpRequestWithBody()
        val okHttpResponse = okHttpResponseWithBody()
        val httpResponse = httpResponse()

        every { httpRequestMapper.toOkHttpRequest(any()) } returns okHttpRequest
        every { httpResponseMapper.toInternalDto(any()) } returns httpResponse
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns okHttpResponse

        val response = httpClient.execute(httpRequest)

        assertEquals(httpResponse, response)
        verify {
            httpRequestMapper.toOkHttpRequest(match {
                RecursiveComparison.isEqualTo(httpRequest, it)
            })
        }
        verify { httpResponseMapper.toInternalDto(okHttpResponse) }
        verify { okHttpClient.newCall(okHttpRequest) }
        verify { call.execute() }
    }

    private fun executeRequestTest(httpRequest: HttpRequest, requestFunction: (httpClient: OkHttpClient) -> HttpResponse) {
        val httpClient = OkHttpClient.Builder()
            .withBaseUrl(Url("https://google.com/"))
            .build()

        val okHttpRequest = okHttpRequestWithBody()
        val okHttpResponse = okHttpResponseWithoutBody()
        val httpResponse = httpResponse()

        every { httpRequestMapper.toOkHttpRequest(any()) } returns okHttpRequest
        every { httpResponseMapper.toInternalDto(any()) } returns httpResponse
        every { okHttpClient.newCall(any()) } returns call
        every { call.execute() } returns okHttpResponse

        val response = requestFunction(httpClient)

        assertEquals(httpResponse, response)
        verify {
            httpRequestMapper.toOkHttpRequest(match {
                RecursiveComparison.isEqualTo(httpRequest, it)
            })
        }
        verify { httpResponseMapper.toInternalDto(okHttpResponse) }
        verify { okHttpClient.newCall(okHttpRequest) }
        verify { call.execute() }
    }

    private fun mockOkHttpClientBuilder() {
        mockkConstructor(okhttp3.OkHttpClient.Builder::class)
        every { anyConstructed<okhttp3.OkHttpClient.Builder>().connectTimeout(any(), any()) } returns okhttp3.OkHttpClient.Builder()
        every { anyConstructed<okhttp3.OkHttpClient.Builder>().readTimeout(any(), any()) } returns okhttp3.OkHttpClient.Builder()
        every { anyConstructed<okhttp3.OkHttpClient.Builder>().writeTimeout(any(), any()) } returns okhttp3.OkHttpClient.Builder()
        every { anyConstructed<okhttp3.OkHttpClient.Builder>().addInterceptor(any<Interceptor>()) } returns okhttp3.OkHttpClient.Builder()
        every { anyConstructed<okhttp3.OkHttpClient.Builder>().build() } returns okHttpClient
    }

    private fun okHttpRequestWithoutBody(): Request {
        return Request.Builder()
            .get().url("https://google.com")
            .header("Name", "Value")
            .build()
    }

    private fun okHttpRequestWithBody(): Request {
        return Request.Builder()
            .post("{\"name\": \"value\"}".toRequestBody("application/json".toMediaType()))
            .url("https://google.com")
            .header("Name", "Value")
            .build()
    }

    private fun okHttpResponseWithoutBody(): Response {
        return Response.Builder()
            .request(
                Request.Builder()
                    .get().url("https://google.com")
                    .header("Name", "Value")
                    .build()
            )
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .build()
    }

    private fun okHttpResponseWithBody(): Response {
        return Response.Builder()
            .request(
                Request.Builder()
                    .get().url("https://google.com")
                    .header("Name", "Value")
                    .build()
            )
            .protocol(Protocol.HTTP_1_1)
            .message("Message")
            .code(200)
            .body("{\"name\": \"value\"}".toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun httpResponse(): HttpResponse {
        return HttpResponse(
            request = HttpRequest(
                url = Url("https://google.com/"),
                method = HttpMethod.GET,
                headers = HttpHeaders(mapOf("Name" to listOf("Value"))),
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            statusCode = 200,
            body = null,
        )
    }
}
