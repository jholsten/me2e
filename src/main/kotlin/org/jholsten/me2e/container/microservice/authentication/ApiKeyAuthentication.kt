package org.jholsten.me2e.container.microservice.authentication

import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.HttpResponse

/**
 * API key authenticator, which sets a fixed API key in the `X-API-KEY` header of each HTTP request.
 * @constructor Instantiates a new API key authenticator.
 * @param apiKey Value of the API key to set in the `X-API-KEY` header.
 */
class ApiKeyAuthentication(
    /**
     * Value of the API key to set in the `X-API-KEY` header.
     */
    private val apiKey: String,
) : Authenticator() {

    /**
     * Returns request interceptor which sets the [apiKey] in the `X-API-KEY` header
     * of each request.
     */
    override fun getRequestInterceptor(): RequestInterceptor {
        return object : RequestInterceptor {
            override fun intercept(chain: RequestInterceptor.Chain): HttpResponse {
                val request = chain.getRequest().newBuilder()
                    .addHeader("X-API-KEY", apiKey)
                    .build()
                return chain.proceed(request)
            }
        }
    }
}
