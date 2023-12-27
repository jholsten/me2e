package org.jholsten.me2e.request.client

import okhttp3.ResponseBody
import org.jholsten.me2e.request.interceptor.OkHttpRequestInterceptor
import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.mapper.HttpResponseMapper
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.HttpResponse
import java.util.concurrent.TimeUnit

/**
 * Client to use for executing requests over HTTP.
 * Uses OkHttp internally for executing and processing the requests.
 */
class OkHttpClient private constructor(
    /**
     * Base URL to set for all requests.
     */
    val baseUrl: String,

    /**
     * Configuration of the HTTP client.
     */
    private val configuration: Configuration,
) : HttpClient {

    override fun setRequestInterceptors(interceptors: List<RequestInterceptor>) {
        configuration.setRequestInterceptors(interceptors).apply()
    }

    override fun get(path: String, queryParams: Map<String, String>, headers: Map<String, List<String>>): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.GET)
            .withHeaders(headers)
            .build()

        return execute(request)
    }

    override fun post(
        path: String,
        body: HttpRequestBody,
        queryParams: Map<String, String>,
        headers: Map<String, List<String>>
    ): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.POST)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun put(
        path: String,
        body: HttpRequestBody,
        queryParams: Map<String, String>,
        headers: Map<String, List<String>>
    ): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.PUT)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun patch(
        path: String,
        body: HttpRequestBody,
        queryParams: Map<String, String>,
        headers: Map<String, List<String>>
    ): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.PATCH)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun delete(
        path: String,
        body: HttpRequestBody?,
        queryParams: Map<String, String>,
        headers: Map<String, List<String>>
    ): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
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

    class Builder : HttpClient.Builder {
        private var baseUrl: String? = null
        private val configuration: Configuration = Configuration()

        override fun withBaseUrl(baseUrl: String) = apply {
            this.baseUrl = baseUrl
        }

        override fun withRequestInterceptors(interceptors: List<RequestInterceptor>) = apply {
            configuration.setRequestInterceptors(interceptors)
        }

        override fun addRequestInterceptor(interceptor: RequestInterceptor) = apply {
            configuration.addRequestInterceptor(interceptor)
        }

        override fun withConnectTimeout(timeout: Long, unit: TimeUnit) = apply {
            configuration.setConnectTimeout(timeout, unit)
        }

        override fun withReadTimeout(timeout: Long, unit: TimeUnit) = apply {
            configuration.setReadTimeout(timeout, unit)
        }

        override fun withWriteTimeout(timeout: Long, unit: TimeUnit) = apply {
            configuration.setWriteTimeout(timeout, unit)
        }

        override fun build(): OkHttpClient {
            val baseUrl = this.baseUrl ?: throw IllegalArgumentException("Base URL cannot be null")
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
        var httpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient()

        /**
         * List of request interceptors to use for outgoing requests.
         */
        var requestInterceptors: MutableList<RequestInterceptor> = mutableListOf()

        /**
         * Connect timeout in milliseconds.
         */
        var connectTimeout: Long = 10000

        /**
         * Read timeout in milliseconds.
         */
        var readTimeout: Long = 10000

        /**
         * Write timeout in milliseconds.
         */
        var writeTimeout: Long = 10000

        override fun setRequestInterceptors(interceptors: List<RequestInterceptor>) = apply {
            requestInterceptors = interceptors.toMutableList()
        }

        override fun addRequestInterceptor(interceptor: RequestInterceptor) = apply {
            requestInterceptors.add(interceptor)
        }

        override fun setConnectTimeout(timeout: Long, unit: TimeUnit) = apply {
            connectTimeout = checkTimeout(timeout, unit)
        }

        override fun setReadTimeout(timeout: Long, unit: TimeUnit) = apply {
            readTimeout = checkTimeout(timeout, unit)
        }

        override fun setWriteTimeout(timeout: Long, unit: TimeUnit) = apply {
            writeTimeout = checkTimeout(timeout, unit)
        }

        /**
         * Applies the configuration to the OkHttp client.
         * Since the client's fields are immutable, a new instance is created.
         */
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
         */
        private fun checkTimeout(timeout: Long, unit: TimeUnit): Long {
            require(timeout >= 0) { "Timeout needs to be a positive value" }
            val millis = unit.toMillis(timeout)
            require(millis <= Integer.MAX_VALUE) { "Timeout in milliseconds needs to be below ${Integer.MAX_VALUE}" }
            return millis
        }
    }
}
