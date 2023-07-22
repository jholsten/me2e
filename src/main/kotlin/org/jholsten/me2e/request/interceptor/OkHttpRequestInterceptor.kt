package org.jholsten.me2e.request.interceptor

import okhttp3.*
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.mapper.HttpResponseMapper
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Interceptor for requests using an okhttp3 client.
 */
internal interface OkHttpRequestInterceptor : Interceptor {
    companion object {
        /**
         * Maps request interceptor to okhttp3 request interceptor.
         * @param interceptor Request interceptor to map
         */
        @JvmStatic
        fun fromRequestInterceptor(interceptor: RequestInterceptor): OkHttpRequestInterceptor {
            return object : OkHttpRequestInterceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val response = interceptor.intercept(OkHttpForwardChain(chain))
                    return HttpResponseMapper.INSTANCE.toOkHttpResponse(response)
                }
            }
        }
    }

    /**
     * Chain that forwards all calls to the okhttp3 chain.
     */
    class OkHttpForwardChain(private val chain: Interceptor.Chain) : RequestInterceptor.Chain {
        override fun getRequest(): HttpRequest {
            val request = chain.request()
            return HttpRequestMapper.INSTANCE.toInternalDto(request)
        }

        override fun proceed(request: HttpRequest): HttpResponse {
            val response = chain.proceed(HttpRequestMapper.INSTANCE.toOkHttpRequest(request))
            return HttpResponseMapper.INSTANCE.toInternalDto(response)
        }
    }
}
