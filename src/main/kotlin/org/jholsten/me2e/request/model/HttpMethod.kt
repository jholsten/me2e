package org.jholsten.me2e.request.model

/**
 * Enum representing the different HTTP methods available for executing requests.
 */
enum class HttpMethod {
    GET,
    PUT,
    POST,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    UNKNOWN;

    /**
     * Returns whether this HTTP method requires a request body.
     */
    fun requiresRequestBody(): Boolean {
        return this == POST || this == PUT || this == PATCH
    }

    /**
     * Returns whether this HTTP method allows having a request body.
     */
    fun allowsRequestBody(): Boolean {
        return this != GET && this != HEAD
    }
}
