package org.jholsten.me2e.request.client

import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.HttpResponse
import java.util.concurrent.TimeUnit

/**
 * Client to use for executing requests over HTTP.
 */
interface HttpClient {

    /**
     * Sets the given interceptors for all outgoing requests.
     */
    fun setRequestInterceptors(interceptors: List<RequestInterceptor>)

    /**
     * Executes an HTTP GET request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun get(path: String, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    /**
     * Executes an HTTP POST request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun post(
        path: String,
        body: HttpRequestBody,
        queryParams: Map<String, String> = mapOf(),
        headers: Map<String, List<String>> = mapOf()
    ): HttpResponse

    /**
     * Executes an HTTP PUT request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun put(
        path: String,
        body: HttpRequestBody,
        queryParams: Map<String, String> = mapOf(),
        headers: Map<String, List<String>> = mapOf()
    ): HttpResponse

    /**
     * Executes an HTTP PATCH request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun patch(
        path: String,
        body: HttpRequestBody,
        queryParams: Map<String, String> = mapOf(),
        headers: Map<String, List<String>> = mapOf()
    ): HttpResponse

    /**
     * Executes an HTTP DELETE request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun delete(
        path: String,
        body: HttpRequestBody? = null,
        queryParams: Map<String, String> = mapOf(),
        headers: Map<String, List<String>> = mapOf()
    ): HttpResponse

    /**
     * Executes the given HTTP request.
     * @param request HTTP request to execute.
     */
    fun execute(request: HttpRequest): HttpResponse

    /**
     * Builder for the HTTP client.
     */
    interface Builder {
        /**
         * Sets the base URL of the client.
         */
        fun withBaseUrl(baseUrl: String): Builder

        /**
         * Sets the given interceptors for all outgoing requests.
         * @param interceptors Request interceptor to set
         */
        fun withRequestInterceptors(interceptors: List<RequestInterceptor>): Builder

        /**
         * Adds the given interceptor for all outgoing requests.
         * @param interceptor Request interceptor to add
         */
        fun addRequestInterceptor(interceptor: RequestInterceptor): Builder

        /**
         * Sets the given connect timeout for new connections. The default value is 10 seconds.
         */
        fun withConnectTimeout(timeout: Long, unit: TimeUnit): Builder

        /**
         * Sets the given read timeout for new connections. The default value is 10 seconds.
         */
        fun withReadTimeout(timeout: Long, unit: TimeUnit): Builder

        /**
         * Sets the given write timeout for new connections. The default value is 10 seconds.
         */
        fun withWriteTimeout(timeout: Long, unit: TimeUnit): Builder

        /**
         * Builds and returns the configured HttpClient.
         */
        fun build(): HttpClient
    }

    /**
     * Configuration of the HTTP client.
     */
    interface Configuration {
        /**
         * Sets the given interceptors for all outgoing requests.
         * @param interceptors Request interceptor to set
         */
        fun setRequestInterceptors(interceptors: List<RequestInterceptor>): Configuration

        /**
         * Adds the given interceptor for all outgoing requests.
         * @param interceptor Request interceptor to add
         */
        fun addRequestInterceptor(interceptor: RequestInterceptor): Configuration

        /**
         * Sets the given connect timeout for new connections. The default value is 10 seconds.
         */
        fun setConnectTimeout(timeout: Long, unit: TimeUnit): Configuration

        /**
         * Sets the given read timeout for new connections. The default value is 10 seconds.
         */
        fun setReadTimeout(timeout: Long, unit: TimeUnit): Configuration

        /**
         * Sets the given write timeout for new connections. The default value is 10 seconds.
         */
        fun setWriteTimeout(timeout: Long, unit: TimeUnit): Configuration
    }
}
