package org.jholsten.me2e.request.client

import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.*
import java.util.concurrent.TimeUnit

/**
 * Client to use for executing requests over HTTP.
 */
interface HttpClient {

    fun setBaseUrl(baseUrl: Url)

    /**
     * Sets the given interceptors for all outgoing requests.
     */
    fun setRequestInterceptors(interceptors: List<RequestInterceptor>)

    fun addRequestInterceptor(interceptor: RequestInterceptor)

    /**
     * Executes an HTTP GET request to the given URL relative to the base URL.
     * Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun get(relativeUrl: RelativeUrl, headers: HttpHeaders): HttpResponse

    /**
     * Executes an HTTP GET request to the given URL relative to the base URL.
     * @param relativeUrl URL of the request relative to the base URL.
     * @return Response returned by the service.
     */
    fun get(relativeUrl: RelativeUrl): HttpResponse {
        return get(relativeUrl, HttpHeaders.empty())
    }

    /**
     * Executes an HTTP POST request to the given URL relative to the base URL.
     * Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun post(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders): HttpResponse

    /**
     * Executes an HTTP POST request to the given URL relative to the base URL.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @return Response returned by the service.
     */
    fun post(relativeUrl: RelativeUrl, body: HttpRequestBody): HttpResponse {
        return post(relativeUrl, body, HttpHeaders.empty())
    }

    /**
     * Executes an HTTP PUT request to the given URl relative to the base URL.
     * Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun put(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders): HttpResponse

    /**
     * Executes an HTTP PUT request to the given URl relative to the base URL.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @return Response returned by the service.
     */
    fun put(relativeUrl: RelativeUrl, body: HttpRequestBody): HttpResponse {
        return put(relativeUrl, body, HttpHeaders.empty())
    }

    /**
     * Executes an HTTP PATCH request to the given URL relative to the base URL.
     * Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun patch(relativeUrl: RelativeUrl, body: HttpRequestBody, headers: HttpHeaders): HttpResponse

    /**
     * Executes an HTTP PATCH request to the given URL relative to the base URL.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @return Response returned by the service.
     */
    fun patch(relativeUrl: RelativeUrl, body: HttpRequestBody): HttpResponse {
        return patch(relativeUrl, body, HttpHeaders.empty())
    }

    /**
     * Executes an HTTP DELETE request to the given URL relative to the base URL.
     * Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun delete(relativeUrl: RelativeUrl, body: HttpRequestBody?, headers: HttpHeaders): HttpResponse

    /**
     * Executes an HTTP DELETE request to the given URL relative to the base URL.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param body Request body to set in the request.
     * @return Response returned by the service.
     */
    fun delete(relativeUrl: RelativeUrl, body: HttpRequestBody?): HttpResponse {
        return delete(relativeUrl, body, HttpHeaders.empty())
    }

    /**
     * Executes an HTTP DELETE request to the given URL relative to the base URL.
     * Sets the given headers if provided.
     * @param relativeUrl URL of the request relative to the base URL.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun delete(relativeUrl: RelativeUrl, headers: HttpHeaders): HttpResponse {
        return delete(relativeUrl, null, headers)
    }

    /**
     * Executes an HTTP DELETE request to the given URL relative to the base URL.
     * @param relativeUrl URL of the request relative to the base URL.
     * @return Response returned by the service.
     */
    fun delete(relativeUrl: RelativeUrl): HttpResponse {
        return delete(relativeUrl, null, HttpHeaders.empty())
    }

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
        fun withBaseUrl(baseUrl: Url): Builder

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
