package org.jholsten.me2e.request.interceptor

import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Request interceptor for observing and modifying outgoing HTTP requests.
 * This may be used for logging purposes or adding authentication headers.
 * @sample org.jholsten.samples.request.basicAuthenticationRequestInterceptor
 */
interface RequestInterceptor {
    /**
     * Entrypoint for intercepting outgoing requests.
     * Call `chain.proceed(request)` in the end to continue the original request.
     */
    fun intercept(chain: Chain): HttpResponse

    /**
     * Base interface for the interceptor chain, which is an ordered list of interceptors
     * associated with specific segments of the message processing pipeline.
     */
    interface Chain {
        /**
         * Returns the outgoing HTTP request.
         */
        fun getRequest(): HttpRequest

        /**
         * Proceeds the interceptor chain.
         * @param request Request with which the chain should proceed.
         */
        fun proceed(request: HttpRequest): HttpResponse
    }
}
