package org.jholsten.me2e.request.client

import okhttp3.ResponseBody
import org.jholsten.me2e.request.interceptor.OkHttpRequestInterceptor
import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.mapper.HttpResponseMapper
import org.jholsten.me2e.request.model.*
import java.util.concurrent.TimeUnit

/**
 * Client to use for executing requests over HTTP.
 * Uses OkHttp internally for executing and processing the requests.
 */
class OkHttpClient private constructor(
    /**
     * Base URL to set for all requests.
     */
    @JvmSynthetic
    internal var baseUrl: Url,

    /**
     * Configuration of the HTTP client.
     */
    private val configuration: Configuration,
) : HttpClient {
    override fun setBaseUrl(baseUrl: Url) {
        this.baseUrl = baseUrl
    }

    override fun setRequestInterceptors(interceptors: List<RequestInterceptor>) {
        configuration.setRequestInterceptors(interceptors).apply()
    }

    override fun addRequestInterceptor(interceptor: RequestInterceptor) {
        configuration.addRequestInterceptor(interceptor).apply()
    }

    override fun get(relativeUrl: RelativeUrl, headers: HttpHeaders): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl.withRelativeUrl(relativeUrl))
            .withMethod(HttpMethod.GET)
            .withHeaders(headers)
            .build()

        return execute(request)
    }

    override fun post(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl.withRelativeUrl(relativeUrl))
            .withMethod(HttpMethod.POST)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun put(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl.withRelativeUrl(relativeUrl))
            .withMethod(HttpMethod.PUT)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun patch(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl.withRelativeUrl(relativeUrl))
            .withMethod(HttpMethod.PATCH)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun delete(relativeUrl: RelativeUrl, body: HttpRequestBody?, headers: HttpHeaders): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl.withRelativeUrl(relativeUrl))
            .withMethod(HttpMethod.DELETE)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun execute(request: HttpRequest): HttpResponse {
        // The response may only be closed, if a body is present (otherwise an [IllegalStateException] is thrown).
        // Therefore, `use` cannot be applied here.
        var responseBody: ResponseBody? = null
        try {
            val response = configuration.httpClient.newCall(HttpRequestMapper.INSTANCE.toOkHttpRequest(request)).execute()
            responseBody = response.body
            return HttpResponseMapper.INSTANCE.toInternalDto(response)
        } finally {
            responseBody?.close()
        }
    }

    class Builder : HttpClient.Builder<Builder> {
        private var baseUrl: Url? = null
        private val configuration: Configuration = Configuration()

        override fun withBaseUrl(baseUrl: Url): Builder = apply {
            this.baseUrl = baseUrl
        }

        override fun withRequestInterceptors(interceptors: List<RequestInterceptor>): Builder = apply {
            configuration.setRequestInterceptors(interceptors)
        }

        override fun addRequestInterceptor(interceptor: RequestInterceptor): Builder = apply {
            configuration.addRequestInterceptor(interceptor)
        }

        override fun withConnectTimeout(timeout: Long, unit: TimeUnit): Builder = apply {
            configuration.setConnectTimeout(timeout, unit)
        }

        override fun withReadTimeout(timeout: Long, unit: TimeUnit): Builder = apply {
            configuration.setReadTimeout(timeout, unit)
        }

        override fun withWriteTimeout(timeout: Long, unit: TimeUnit): Builder = apply {
            configuration.setWriteTimeout(timeout, unit)
        }

        override fun build(): OkHttpClient {
            val baseUrl = requireNotNull(this.baseUrl) { "Base URL cannot be null" }
            configuration.apply()
            return OkHttpClient(
                baseUrl = baseUrl,
                configuration = configuration,
            )
        }
    }

    internal class Configuration : HttpClient.Configuration {
        /**
         * Configured client which executes the requests.
         */
        @JvmSynthetic
        var httpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient()

        /**
         * List of request interceptors to use for outgoing requests.
         */
        @JvmSynthetic
        var requestInterceptors: MutableList<RequestInterceptor> = mutableListOf()

        /**
         * Connect timeout in milliseconds.
         */
        @JvmSynthetic
        var connectTimeout: Long = 10000

        /**
         * Read timeout in milliseconds.
         */
        @JvmSynthetic
        var readTimeout: Long = 10000

        /**
         * Write timeout in milliseconds.
         */
        @JvmSynthetic
        var writeTimeout: Long = 10000

        @JvmSynthetic
        override fun setRequestInterceptors(interceptors: List<RequestInterceptor>): Configuration = apply {
            requestInterceptors = interceptors.toMutableList()
        }

        @JvmSynthetic
        override fun addRequestInterceptor(interceptor: RequestInterceptor): Configuration = apply {
            requestInterceptors.add(interceptor)
        }

        @JvmSynthetic
        override fun setConnectTimeout(timeout: Long, unit: TimeUnit): Configuration = apply {
            connectTimeout = checkTimeout(timeout, unit)
        }

        @JvmSynthetic
        override fun setReadTimeout(timeout: Long, unit: TimeUnit): Configuration = apply {
            readTimeout = checkTimeout(timeout, unit)
        }

        @JvmSynthetic
        override fun setWriteTimeout(timeout: Long, unit: TimeUnit): Configuration = apply {
            writeTimeout = checkTimeout(timeout, unit)
        }

        /**
         * Applies the configuration to the OkHttp client.
         * Since the client's fields are immutable, a new instance is created.
         */
        @JvmSynthetic
        fun apply() {
            val builder = okhttp3.OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)

            for (interceptor in requestInterceptors) {
                builder.addInterceptor(OkHttpRequestInterceptor.fromRequestInterceptor(interceptor))
            }

            httpClient = builder.build()
        }

        /**
         * Asserts that the given timeout value is valid (with respect to the requirements of okhttp).
         * Returns timeout value in milliseconds.
         * @throws IllegalArgumentException if timeout value is out of range.
         */
        private fun checkTimeout(timeout: Long, unit: TimeUnit): Long {
            require(timeout >= 0) { "Timeout needs to be a positive value" }
            val millis = unit.toMillis(timeout)
            require(millis <= Integer.MAX_VALUE) { "Timeout in milliseconds needs to be below ${Integer.MAX_VALUE}" }
            return millis
        }
    }
}
