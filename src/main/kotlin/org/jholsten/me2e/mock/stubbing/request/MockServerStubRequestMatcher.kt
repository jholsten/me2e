package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.RequestMethod
import org.jholsten.me2e.config.parser.deserializer.MockServerDeserializer
import org.jholsten.me2e.mock.verification.MatchResult
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.utils.toJson

/**
 * Definition of the request to which the stub should respond.
 * Matches the request depending on certain patterns.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class MockServerStubRequestMatcher internal constructor(
    /**
     * Hostname of the third-party service to be mocked.
     * The value is set to the [org.jholsten.me2e.mock.MockServer]'s hostname while deserializing.
     * @see org.jholsten.me2e.config.parser.deserializer.MockServerDeserializer.parseStubFiles
     */
    @JacksonInject(MockServerDeserializer.INJECTABLE_HOSTNAME_FIELD_NAME)
    val hostname: String,

    /**
     * HTTP method of the request to which the stub should respond. A value of `null` indicates that the
     * request method can be ignored and this stub should respond to all methods.
     */
    val method: HttpMethod? = null,

    /**
     * URL path of the request to which the stub should respond. A value of `null` indicates that the path
     * can be ignored and this stub should respond to all paths.
     * To be able to constrain the match to specific path variables, use [StringMatcher.matches] with an
     * appropriate regular expression.
     */
    val path: StringMatcher? = null,

    /**
     * Headers of the request to which the stub should respond as map of case-insensitive header name and string matcher.
     * A value of `null` or an empty map indicates that the request headers can be ignored and this stub
     * should respond to all headers.
     */
    val headers: Map<String, StringMatcher>? = null,

    /**
     * Query parameters of the request to which the stub should respond as map of case-insensitive query parameter name
     * and string matcher for the values. A value of `null` or an empty map indicates that the query parameters can be
     * ignored and this stub should respond to all query parameters.
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
     * @param request Actual request that the Mock Server received.
     * @return Whether the given request matches this request pattern.
     */
    @JvmSynthetic
    internal fun matches(request: Request): MatchResult {
        var matches = true
        val messages: MutableList<String> = mutableListOf()
        val hostnameMatchResult = this.hostnameMatches(request.host)
        if (!hostnameMatchResult.matches) {
            matches = false
            messages.addAll(hostnameMatchResult.failures)
        }
        val methodMatchResult = this.methodMatches(request.method)
        if (!methodMatchResult.matches) {
            matches = false
            messages.addAll(methodMatchResult.failures)
        }
        val pathMatchResult = this.pathMatches(request.url)
        if (!pathMatchResult.matches) {
            matches = false
            messages.addAll(pathMatchResult.failures)
        }
        val headersMatchResult = this.headersMatch(request.headers)
        if (!headersMatchResult.matches) {
            matches = false
            messages.addAll(headersMatchResult.failures)
        }
        val queryParametersMatchResult = this.queryParametersMatch(request)
        if (!queryParametersMatchResult.matches) {
            matches = false
            messages.addAll(queryParametersMatchResult.failures)
        }
        val bodyPatternsMatchResult = this.bodyPatternsMatch(request)
        if (!bodyPatternsMatchResult.matches) {
            matches = false
            messages.addAll(bodyPatternsMatchResult.failures)
        }

        return MatchResult(
            matches = matches,
            failures = messages,
        )
    }

    /**
     * Returns whether the actual hostname of the request matches the [hostname] defined for this stub.
     * @param actualHostname Actual hostname of the captured request.
     * @return Whether the actual hostname matches the hostname defined for this stub.
     */
    @JvmSynthetic
    internal fun hostnameMatches(actualHostname: String): MatchResult {
        val matches = this.hostname == actualHostname
        return MatchResult(
            matches = matches,
            failures = when (matches) {
                true -> listOf()
                false -> listOf("Expected hostname\n\t$actualHostname\nto be equal to\n\t$hostname")
            },
        )
    }

    /**
     * Returns whether the actual request method of the request matches the [method] defined for this stub.
     * @param actualMethod Actual request method of the captured request.
     * @return True if either the [method] defined for this stub should be ignored or if the actual method
     * of the captured request is equal to the [method] defined for this stub.
     */
    @JvmSynthetic
    internal fun methodMatches(actualMethod: RequestMethod): MatchResult {
        if (this.method == null) {
            return MatchResult(true)
        }

        val matches = this.method.name == actualMethod.name
        return MatchResult(
            matches = matches,
            failures = when (matches) {
                true -> listOf()
                false -> listOf("Expected method\n\t${actualMethod.name}\nto be equal to\n\t${this.method.name}")
            },
        )
    }

    /**
     * Returns whether the actual path of the request matches the [path] defined for this stub. Ignores query parameters.
     * @param actualUrl Actual path of the captured request.
     * @return True if either the [path] defined for this stub should be ignored or if the actual path
     * of the captured request is equal to the [path] defined for this stub.
     */
    @JvmSynthetic
    internal fun pathMatches(actualUrl: String): MatchResult {
        if (this.path == null) {
            return MatchResult(true)
        }

        val actualPath = when {
            actualUrl.contains("?") -> actualUrl.substring(0, actualUrl.indexOf("?"))
            else -> actualUrl
        }

        val matches = this.path.matches(actualPath)
        return MatchResult(
            matches = matches,
            failures = when (matches) {
                true -> listOf()
                false -> listOf("Expected path\n\t$actualPath\nto match\n\t$path")
            },
        )
    }

    /**
     * Returns whether the actual headers of the request match the [headers] defined for this stub.
     * The headers are considered as matched if each of the [headers] is present in the actual request and their values
     * are as defined in the corresponding [StringMatcher]. Additional headers in the actual request are ignored.
     * The keys of the headers are case-insensitive.
     * @param headers Actual headers of the captured request.
     * @return True if either the [headers] defined for this stub should be ignored or if the actual headers
     * of the captured request match the [headers] defined for this stub.
     */
    @JvmSynthetic
    internal fun headersMatch(headers: HttpHeaders?): MatchResult {
        if (this.headers.isNullOrEmpty()) {
            return MatchResult(true)
        } else if (headers == null) {
            return MatchResult(false, listOf("Expected headers\n\tnull\nto match\n\t${this.headers}"))
        }

        var matches = true
        val messages: MutableList<String> = mutableListOf()
        for (header in this.headers) {
            val actualHeader = headers.getHeader(header.key.lowercase())
            if (!actualHeader.isPresent) {
                matches = false
                messages.add("Expected headers to contain key ${header.key}")
            } else if (!actualHeader.values().any { header.value.matches(it) }) {
                matches = false
                messages.add(
                    "Expected headers to contain key ${header.key} with values\n\t${actualHeader.values()}\n" +
                        "to contain at least one value which matches\n\t${header.value}"
                )
            }
        }

        return MatchResult(
            matches = matches,
            failures = messages,
        )
    }

    /**
     * Returns whether the actual query parameters of the request match the [queryParameters] defined for this stub.
     * The query parameters are considered as matched if each of the [queryParameters] is present in the actual request
     * and their values are as defined in the corresponding [StringMatcher]. Additional query parameters in the actual
     * request are ignored. The keys of the query parameters are case-insensitive.
     * @param request Actual captured request.
     * @return True if either the [queryParameters] defined for this stub should be ignored or if the actual query
     * parameters of the captured request match the [queryParameters] defined for this stub.
     */
    @JvmSynthetic
    internal fun queryParametersMatch(request: Request): MatchResult {
        if (this.queryParameters.isNullOrEmpty()) {
            return MatchResult(true)
        }

        var matches = true
        val messages: MutableList<String> = mutableListOf()
        for (parameter in this.queryParameters) {
            val actualParameter = request.queryParameter(parameter.key.lowercase())
            if (!actualParameter.isPresent) {
                matches = false
                messages.add("Expected query parameters to contain key ${parameter.key}")
            } else if (!actualParameter.values().any { parameter.value.matches(it) }) {
                matches = false
                messages.add(
                    "Expected query parameters to contain key ${parameter.key} with values\n\t${actualParameter.values()}\n" +
                        "to contain at least one value which matches\n\t${parameter.value}"
                )
            }
        }

        return MatchResult(
            matches = matches,
            failures = messages,
        )
    }

    /**
     * Returns whether the actual body of the request matches the [bodyPatterns] defined for this stub.
     * @param request Actual captured request.
     * @return True if either the [bodyPatterns] defined for this stub should be ignored or if the actual body
     * of the captured request match all the [bodyPatterns] defined for this stub.
     */
    @JvmSynthetic
    internal fun bodyPatternsMatch(request: Request): MatchResult {
        if (this.bodyPatterns.isNullOrEmpty()) {
            return MatchResult(true)
        }

        val body = request.bodyAsString
        val messages: MutableList<String> = mutableListOf()
        var matches = true
        for (pattern in this.bodyPatterns) {
            if (!pattern.matches(body)) {
                matches = false
                messages.add("Expected body\n\t$body\n\tto match\n\t$pattern")
            }
        }
        return MatchResult(
            matches = matches,
            failures = messages,
        )
    }

    override fun toString(): String = toJson(this)
}
