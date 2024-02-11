package org.jholsten.me2e.request.client

import org.jholsten.me2e.request.interceptor.RequestInterceptor
import org.jholsten.me2e.request.model.*
import java.util.concurrent.TimeUnit

/**
 * Client to use for executing requests over HTTP.
 * @see OkHttpClient
 */
interface HttpClient {

    /**
     * Updates the base URL to the given value.
     * @param baseUrl Base URL to set for this HTTP client.
     */
    fun setBaseUrl(baseUrl: Url)

    /**
     * Sets the given interceptors for all outgoing requests.
     * Note that this overwrites all existing interceptors. If you want to add an interceptor instead,
     * use [addRequestInterceptor].
     * @param interceptors Request interceptors to set.
     */
    fun setRequestInterceptors(interceptors: List<RequestInterceptor>)

    /**
     * Adds the given interceptor for all outgoing requests to the existing list of interceptors.
     * @param interceptor Request interceptor to add.
     */
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
    interface Builder<SELF : Builder<SELF>> {
        /**
         * Sets the base URL of the client.
         * @param baseUrl Base URL to set.
         * @return This builder instance, to use for chaining.
         */
        fun withBaseUrl(baseUrl: Url): SELF

        /**
         * Sets the given interceptors for all outgoing requests.
         * @param interceptors Request interceptor to set.
         * @return This builder instance, to use for chaining.
         */
        fun withRequestInterceptors(interceptors: List<RequestInterceptor>): SELF

        /**
         * Adds the given interceptor for all outgoing requests.
         * @param interceptor Request interceptor to add.
         * @return This builder instance, to use for chaining.
         */
        fun addRequestInterceptor(interceptor: RequestInterceptor): SELF

        /**
         * Sets the given connect timeout for new connections. The default value is 10 seconds.
         * @param timeout Connect timeout in [unit] to set.
         * @param unit Time unit of the [timeout] value to set.
         * @return This builder instance, to use for chaining.
         */
        fun withConnectTimeout(timeout: Long, unit: TimeUnit): SELF

        /**
         * Sets the given read timeout for new connections. The default value is 10 seconds.
         * @param timeout Read timeout in [unit] to set.
         * @param unit Time unit of the [timeout] value to set.
         * @return This builder instance, to use for chaining.
         */
        fun withReadTimeout(timeout: Long, unit: TimeUnit): SELF

        /**
         * Sets the given write timeout for new connections. The default value is 10 seconds.
         * @param timeout Write timeout in [unit] to set.
         * @param unit Time unit of the [timeout] value to set.
         * @return This builder instance, to use for chaining.
         */
        fun withWriteTimeout(timeout: Long, unit: TimeUnit): SELF

        /**
         * Set whether to retry requests when a connectivity problem is encountered.
         * @param retryOnConnectionFailure Whether to retry requests when a connectivity
         * problem is encountered.
         * @return This builder instance, to use for chaining.
         */
        fun withRetryOnConnectionFailure(retryOnConnectionFailure: Boolean): SELF

        /**
         * Builds an instance of the [HttpClient] using the properties set in this builder.
         */
        fun build(): HttpClient
    }

    /**
     * Configuration of the HTTP client.
     * Enables to modify an existing HTTP client instance.
     */
    interface Configuration {
        /**
         * Sets the given interceptors for all outgoing requests.
         * @param interceptors Request interceptor to set.
         */
        fun setRequestInterceptors(interceptors: List<RequestInterceptor>): Configuration

        /**
         * Adds the given interceptor for all outgoing requests.
         * @param interceptor Request interceptor to add.
         */
        fun addRequestInterceptor(interceptor: RequestInterceptor): Configuration

        /**
         * Sets the given connect timeout for new connections. The default value is 10 seconds.
         * @param timeout Connect timeout in [unit] to set.
         * @param unit Time unit of the [timeout] value to set.
         */
        fun setConnectTimeout(timeout: Long, unit: TimeUnit): Configuration

        /**
         * Sets the given read timeout for new connections. The default value is 10 seconds.
         * @param timeout Read timeout in [unit] to set.
         * @param unit Time unit of the [timeout] value to set.
         */
        fun setReadTimeout(timeout: Long, unit: TimeUnit): Configuration

        /**
         * Sets the given write timeout for new connections. The default value is 10 seconds.
         * @param timeout Write timeout in [unit] to set.
         * @param unit Time unit of the [timeout] value to set.
         */
        fun setWriteTimeout(timeout: Long, unit: TimeUnit): Configuration

        /**
         * Set whether to retry requests when a connectivity problem is encountered.
         * @param retryOnConnectionFailure Whether to retry requests when a connectivity
         * problem is encountered.
         */
        fun setRetryOnConnectionFailure(retryOnConnectionFailure: Boolean): Configuration
    }
}
