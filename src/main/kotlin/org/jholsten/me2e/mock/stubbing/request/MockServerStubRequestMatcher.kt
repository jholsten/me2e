package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import org.jholsten.me2e.request.model.HttpMethod

/**
 * Definition of the request to which the stub should respond.
 * Matches the request depending on certain patterns.
 */
class MockServerStubRequestMatcher(
    /**
     * HTTP method of the request
     */
    val method: HttpMethod?,

    /**
     * URL path of the request
     */
    val path: StringMatcher?,

    /**
     * Headers of the request as map of header name and string matcher
     */
    val headers: Map<String, StringMatcher>? = null,

    /**
     * Query parameters of the request as map of query parameter name and string matcher for the values
     */
    @JsonProperty("query-parameters")
    val queryParameters: Map<String, StringMatcher>? = null,

    /**
     * Patterns to match the request body
     */
    @JsonProperty("body-patterns")
    val bodyPatterns: List<StringMatcher>? = null,
) {
    companion object {
        /**
         * Returns matcher that matches any request that the mock server receives.
         */
        @JvmStatic
        fun any(): MockServerStubRequestMatcher {
            return MockServerStubRequestMatcher(
                method = null,
                path = null,
                headers = null,
                queryParameters = null,
                bodyPatterns = null,
            )
        }
    }

    class Builder(
        val method: HttpMethod,
        val path: StringMatcher,
    ) {
        private var headers: Map<String, StringMatcher>? = null
        private var queryParameters: Map<String, StringMatcher>? = null
        private var bodyPatterns: List<StringMatcher>? = null

        fun withHeaders(headers: Map<String, StringMatcher>): Builder = apply {
            this.headers = headers
        }

        fun withQueryParameters(queryParameters: Map<String, StringMatcher>): Builder = apply {
            this.queryParameters = queryParameters
        }

        fun withBodyPatterns(bodyPatterns: List<StringMatcher>): Builder = apply {
            this.bodyPatterns = bodyPatterns
        }

        fun build(): MockServerStubRequestMatcher {
            return MockServerStubRequestMatcher(
                method = this.method,
                path = this.path,
                headers = this.headers,
                queryParameters = this.queryParameters,
                bodyPatterns = this.bodyPatterns,
            )
        }
    }

    /**
     * Returns whether the given WireMock request matches the requirements of this stub request.
     * @param request Actual request that was executed.
     */
    internal fun matches(request: Request): Boolean {
        if (!methodMatches(request.method)) {
            return false
        } else if (!pathMatches(request.url)) {
            return false
        } else if (!headersMatch(request.headers)) {
            return false
        } else if (!queryParametersMatch(request)) {
            return false
        } else if (!bodyPatternsMatch(request)) {
            return false
        }

        return true
    }

    internal fun methodMatches(actualMethod: RequestMethod): Boolean {
        if (this.method == null) {
            return true
        }

        return this.method.name == actualMethod.name
    }

    internal fun pathMatches(actualUrl: String): Boolean {
        if (this.path == null) {
            return true
        }

        return this.path.matches(actualUrl, isUrl = true)
    }

    internal fun headersMatch(headers: HttpHeaders?): Boolean {
        if (this.headers.isNullOrEmpty()) {
            return true
        } else if (headers == null) {
            return false
        }

        for (header in this.headers) {
            val actualHeader = headers.getHeader(header.key)
            if (!actualHeader.isPresent || !actualHeader.values().any { header.value.matches(it) }) {
                return false
            }
        }

        return true
    }

    internal fun queryParametersMatch(request: Request): Boolean {
        if (this.queryParameters.isNullOrEmpty()) {
            return true
        }

        for (parameter in this.queryParameters) {
            val actualParameter = request.queryParameter(parameter.key)
            if (!actualParameter.isPresent || !actualParameter.values().any { parameter.value.matches(it) }) {
                return false
            }
        }

        return true
    }

    internal fun bodyPatternsMatch(request: Request): Boolean {
        if (this.bodyPatterns == null || this.bodyPatterns.isEmpty()) {
            return true
        }

        val body = request.bodyAsString
        return this.bodyPatterns.all { it.matches(body) }
    }
}
