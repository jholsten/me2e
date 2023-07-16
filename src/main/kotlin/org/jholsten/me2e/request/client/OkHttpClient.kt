package org.jholsten.me2e.request.client

import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.mapper.HttpResponseMapper
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Client to use for executing requests over HTTP.
 * Uses OkHttp internally for executing and processing the requests.
 */
class OkHttpClient(
    /**
     * Base URL to set for all requests.
     */
    val baseUrl: String,
) : HttpClient {
    /**
     * Configured client which executes the requests.
     * TODO: How to configure the client?
     */
    private val httpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient()

    // TODO: @JvmOverloads
    override fun get(path: String, queryParams: Map<String, String>, headers: Map<String, List<String>>): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.GET)
            .withHeaders(headers)
            .build()

        return execute(request)
    }

    override fun post(path: String, body: HttpRequestBody, queryParams: Map<String, String>, headers: Map<String, List<String>>): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.POST)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun put(path: String, body: HttpRequestBody, queryParams: Map<String, String>, headers: Map<String, List<String>>): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.PUT)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun patch(path: String, body: HttpRequestBody, queryParams: Map<String, String>, headers: Map<String, List<String>>): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.PATCH)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun delete(path: String, body: HttpRequestBody?, queryParams: Map<String, String>, headers: Map<String, List<String>>): HttpResponse {
        val request = HttpRequest.Builder()
            .withUrl(baseUrl, relativePath = path, queryParams = queryParams)
            .withMethod(HttpMethod.DELETE)
            .withHeaders(headers)
            .withBody(body)
            .build()

        return execute(request)
    }

    override fun execute(request: HttpRequest): HttpResponse {
        httpClient.newCall(HttpRequestMapper.INSTANCE.toOkHttpRequest(request)).execute().use {
            return HttpResponseMapper.INSTANCE.toInternalDto(it)
        }
    }
}
