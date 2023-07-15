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

    fun post(path: String, body: HttpRequestBody, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    fun put(path: String, body: HttpRequestBody, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    fun patch(path: String, body: HttpRequestBody, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    fun delete(path: String, body: HttpRequestBody? = null, queryParams: Map<String, String> = mapOf(), headers: Map<String, List<String>> = mapOf()): HttpResponse

    fun execute(request: HttpRequest): HttpResponse
}
