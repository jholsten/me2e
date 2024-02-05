package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.utils.toJson

/**
 * Definition of the request to which the stub should respond.
 * Matches the request depending on certain patterns.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class MockServerStubRequestMatcher(
    /**
     * Hostname of the third-party service to be mocked.
     * The value is set to the [org.jholsten.me2e.mock.MockServer]'s hostname while deserializing.
     * @see org.jholsten.me2e.config.utils.MockServerDeserializer.parseStubFiles
     */
    @JacksonInject("hostname")
    val hostname: String,

    /**
     * HTTP method of the request to which the stub should respond. A value of `null` indicates that the
     * request method can be ignored and this stub should respond to all methods.
     */
    val method: HttpMethod? = null,

    /**
     * URL path of the request to which the stub should respond. A value of `null` indicates that the
     * path can be ignored and this stub should respond to all paths.
     */
    val path: StringMatcher? = null,

    /**
     * Headers of the request to which the stub should respond as map of header name and string matcher.
     * A value of `null` or an empty map indicates that the request headers can be ignored and this stub
     * should respond to all headers.
     */
    val headers: Map<String, StringMatcher>? = null,

    /**
     * Query parameters of the request to which the stub should respond as map of query parameter name and string
     * matcher for the values. A value of `null` or an empty map indicates that the query parameters can be ignored
     * and this stub should respond to all query parameters.
     */
    @JsonProperty("query-parameters")
    val queryParameters: Map<String, StringMatcher>? = null,

    /**
     * Patterns to match the request body to which the stub should respond. A value of `null` or an empty list
     * indicates that the request body can be ignored and this stub should respond to all methods.
     */
    @JsonProperty("body-patterns")
    val bodyPatterns: List<StringMatcher>? = null,
) {

    /**
     * Returns whether the given WireMock request matches the requirements of this stub request.
     * @param request Actual request that the mock server received.
     * @return Whether the given request matches this request pattern.
     */
    @JvmSynthetic
    internal fun matches(request: Request): Boolean {
        if (!hostnameMatches(request.host)) {
            return false
        } else if (!methodMatches(request.method)) {
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

    /**
     * Returns whether the actual hostname of the request matches the [hostname] defined for this stub.
     * @param actualHostname Actual hostname of the captured request.
     * @return Whether the actual hostname matches the hostname defined for this stub.
     */
    @JvmSynthetic
    internal fun hostnameMatches(actualHostname: String): Boolean {
        return this.hostname == actualHostname
    }

    /**
     * Returns whether the actual request method of the request matches the [method] defined for this stub.
     * @param actualMethod Actual request method of the captured request.
     * @return True if either the [method] defined for this stub should be ignored or if the actual method
     * of the captured request is equal to the [method] defined for this stub.
     */
    @JvmSynthetic
    internal fun methodMatches(actualMethod: RequestMethod): Boolean {
        if (this.method == null) {
            return true
        }

        return this.method.name == actualMethod.name
    }

    /**
     * Returns whether the actual path of the request matches the [path] defined for this stub. Ignores query parameters.
     * @param actualUrl Actual path of the captured request.
     * @return True if either the [path] defined for this stub should be ignored or if the actual path
     * of the captured request is equal to the [path] defined for this stub.
     */
    @JvmSynthetic
    internal fun pathMatches(actualUrl: String): Boolean {
        if (this.path == null) {
            return true
        }

        val actualPath = when {
            actualUrl.contains("?") -> actualUrl.substring(0, actualUrl.indexOf("?"))
            else -> actualUrl
        }
        return this.path.matches(actualPath)
    }

    /**
     * Returns whether the actual headers of the request match the [headers] defined for this stub.
     * The headers are considered as matched if each of the [headers] is present in the actual request and their values
     * are as defined in the corresponding [StringMatcher]. Additional headers in the actual request are ignored.
     * @param headers Actual headers of the captured request.
     * @return True if either the [headers] defined for this stub should be ignored or if the actual headers
     * of the captured request match the [headers] defined for this stub.
     */
    @JvmSynthetic
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

    /**
     * Returns whether the actual query parameters of the request match the [queryParameters] defined for this stub.
     * The query parameters are considered as matched if each of the [queryParameters] is present in the actual request
     * and their values are as defined in the corresponding [StringMatcher]. Additional query parameters in the actual
     * request are ignored.
     * @param request Actual captured request.
     * @return True if either the [queryParameters] defined for this stub should be ignored or if the actual query
     * parameters of the captured request match the [queryParameters] defined for this stub.
     */
    @JvmSynthetic
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

    /**
     * Returns whether the actual body of the request matches the [bodyPatterns] defined for this stub.
     * @param request Actual captured request.
     * @return True if either the [bodyPatterns] defined for this stub should be ignored or if the actual body
     * of the captured request match all the [bodyPatterns] defined for this stub.
     */
    @JvmSynthetic
    internal fun bodyPatternsMatch(request: Request): Boolean {
        if (this.bodyPatterns.isNullOrEmpty()) {
            return true
        }

        val body = request.bodyAsString
        return this.bodyPatterns.all { it.matches(body) }
    }

    override fun toString(): String = toJson(this)
}
