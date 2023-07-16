package org.jholsten.me2e.request.client

import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Client to use for executing requests over HTTP.
 */
interface HttpClient {

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
    fun post(path: String, body: HttpRequestBody, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    /**
     * Executes an HTTP PUT request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun put(path: String, body: HttpRequestBody, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    /**
     * Executes an HTTP PATCH request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun patch(path: String, body: HttpRequestBody, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    /**
     * Executes an HTTP DELETE request to the given path.
     * Sets the given headers and query params if provided.
     * @param path Path to send the request to, relative to the service's base URL.
     * @param body Request body to set in the request.
     * @param queryParams Map of query parameter names along with the associated values to set.
     * @param headers Map of header names along with the associated values to set.
     * @return Response returned by the service.
     */
    fun delete(path: String, body: HttpRequestBody? = null, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    /**
     * Executes the given HTTP request.
     * @param request HTTP request to execute.
     */
    fun execute(request: HttpRequest): HttpResponse
}
