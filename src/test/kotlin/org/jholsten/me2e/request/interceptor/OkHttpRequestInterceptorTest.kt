package org.jholsten.me2e.request.interceptor

import io.mockk.*
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.mapper.HttpResponseMapper
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.model.Url
import org.jholsten.util.RecursiveComparison
import org.slf4j.Logger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OkHttpRequestInterceptorTest {

    private val chain = mockk<Interceptor.Chain>()
    private val httpRequestMapper = mockk<HttpRequestMapper>()
    private val httpResponseMapper = mockk<HttpResponseMapper>()

    @BeforeTest
    fun beforeTest() {
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
    fun `Interceptor should call okhttp3 chain`() {
        val logger = mockk<Logger>()
        val interceptor = object : RequestInterceptor {
            override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                logger.info("Executing request...")
                return chain.proceed(chain.getRequest())
            }
        }
        val okHttpRequest = okHttpRequest()
        val httpRequest = httpRequest()
        val okHttpResponse = okHttpResponse()
        val httpResponse = httpResponse()

        every { logger.info(any()) } just runs
        every { chain.request() } returns okHttpRequest
        every { chain.proceed(any()) } returns okHttpResponse
        every { httpRequestMapper.toInternalDto(any<Request>()) } returns httpRequest
        every { httpRequestMapper.toOkHttpRequest(any()) } returns okHttpRequest
        every { httpResponseMapper.toInternalDto(any()) } returns httpResponse
        every { httpResponseMapper.toOkHttpResponse(any()) } returns okHttpResponse

        val okHttpRequestInterceptor = OkHttpRequestInterceptor.fromRequestInterceptor(interceptor)
        val response = okHttpRequestInterceptor.intercept(this.chain)

        assertEquals(okHttpResponse, response)
        verify { logger.info("Executing request...") }
        verify { chain.request() }
        verify { chain.proceed(okHttpRequest) }
        verify { httpRequestMapper.toInternalDto(okHttpRequest) }
        verify { httpRequestMapper.toOkHttpRequest(httpRequest) }
        verify { httpResponseMapper.toInternalDto(okHttpResponse) }
        verify { httpResponseMapper.toOkHttpResponse(httpResponse) }
    }

    @Test
    fun `Modifying interceptor should call okhttp3 chain`() {
        val interceptor = object : RequestInterceptor {
            override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                val request = chain.getRequest().newBuilder()
                    .addHeader("Authorization", "Bearer 123")
                    .build()
                return chain.proceed(request)
            }
        }
        val originalOkHttpRequest = okHttpRequest()
        val httpRequest = httpRequest()
        val okHttpResponse = okHttpResponse()
        val httpResponse = httpResponse()
        val modifiedOkHttpRequest = originalOkHttpRequest.newBuilder()
            .addHeader("Authorization", "Bearer 123")
            .build()
        val modifiedHttpRequest = httpRequest.newBuilder()
            .addHeader("Authorization", "Bearer 123")
            .build()

        every { chain.request() } returns originalOkHttpRequest
        every { chain.proceed(any()) } returns okHttpResponse
        every { httpRequestMapper.toInternalDto(any<Request>()) } returns httpRequest
        every { httpRequestMapper.toOkHttpRequest(any()) } returns modifiedOkHttpRequest
        every { httpResponseMapper.toInternalDto(any()) } returns httpResponse
        every { httpResponseMapper.toOkHttpResponse(any()) } returns okHttpResponse

        val okHttpRequestInterceptor = OkHttpRequestInterceptor.fromRequestInterceptor(interceptor)
        val response = okHttpRequestInterceptor.intercept(this.chain)

        assertEquals(okHttpResponse, response)
        verify { chain.request() }
        verify { chain.proceed(modifiedOkHttpRequest) }
        verify { httpRequestMapper.toInternalDto(originalOkHttpRequest) }
        verify {
            httpRequestMapper.toOkHttpRequest(match {
                RecursiveComparison.isEqualTo(modifiedHttpRequest, it)
            })
        }
        verify { httpResponseMapper.toInternalDto(okHttpResponse) }
        verify { httpResponseMapper.toOkHttpResponse(httpResponse) }
    }

    private fun okHttpRequest(): Request {
        return Request.Builder()
            .get().url("https://google.com")
            .header("Name", "Value")
            .build()
    }

    private fun httpRequest(): HttpRequest {
        return HttpRequest(
            url = Url("https://google.com/"),
            method = HttpMethod.GET,
            headers = mapOf("Name" to listOf("Value")),
            body = null,
        )
    }

    private fun okHttpResponse(): Response {
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

    private fun httpResponse(): HttpResponse {
        return HttpResponse(
            request = HttpRequest(
                url = Url("https://google.com/"),
                method = HttpMethod.GET,
                headers = mapOf("Name" to listOf("Value")),
                body = null,
            ),
            protocol = "http/1.1",
            message = "Message",
            code = 200,
            body = null,
        )
    }
}
