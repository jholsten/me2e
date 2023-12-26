package org.jholsten.me2e.mock.verification

import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.request.model.HttpMethod

/**
 * Model for verifying that a mock server received a certain request.
 */
class MockServerVerification internal constructor(
    /**
     * Number of times that the mock server should have received this request.
     * If set to `null`, it is verified that the mock server received the specified request at least once.
     */
    internal val times: Int?,
) {
    companion object {
        /**
         * Entrypoint for specifying a request that a mock server should have received at least once.
         */
        @JvmStatic
        fun receivedRequest(): MockServerVerification {
            return MockServerVerification(null)
        }

        /**
         * Entrypoint for specifying a request that a mock server should have received exactly [times] number of times.
         * @param times Number of times that the mock server should have received this request.
         */
        @JvmStatic
        fun receivedRequest(times: Int): MockServerVerification {
            return MockServerVerification(times)
        }
    }

    internal var method: HttpMethod? = null
    internal var path: StringMatcher? = null
    internal var headers: MutableMap<String, StringMatcher>? = null
    internal var queryParameters: MutableMap<String, StringMatcher>? = null
    internal var requestBodyPattern: StringMatcher? = null
    internal var noOther: Boolean = false

    /**
     * HTTP method that the expected incoming request should have.
     * Not setting this value means that the HTTP method of the expected request is arbitrary.
     */
    fun withMethod(method: HttpMethod): MockServerVerification = apply {
        this.method = method
    }

    /**
     * Path that the expected incoming request should have.
     * Not setting this value means that the path of the expected request is arbitrary.
     */
    fun withPath(path: StringMatcher): MockServerVerification = apply {
        this.path = path
    }

    /**
     * Header that the expected incoming request should include.
     * Not setting this value means that the headers of the expected request are arbitrary.
     */
    fun withHeader(key: String, headerValue: StringMatcher) = apply {
        if (this.headers == null) {
            this.headers = mutableMapOf()
        }
        this.headers!![key] = headerValue
    }

    /**
     * Query parameter that the expected incoming request should include.
     * Not setting this value means that the query parameters of the expected request are arbitrary.
     */
    fun withQueryParameter(key: String, queryParameterValue: StringMatcher) = apply {
        if (this.queryParameters == null) {
            this.queryParameters = mutableMapOf()
        }
        this.queryParameters!![key] = queryParameterValue
    }

    /**
     * Request body that the expected incoming request should have.
     * Not setting this value means that the request body of the expected request is arbitrary.
     */
    fun withRequestBody(bodyMatcher: StringMatcher) = apply {
        this.requestBodyPattern = bodyMatcher
    }

    /**
     * Expect that the mock server only received this specified request and no other.
     */
    fun andNoOther() = apply {
        this.noOther = true
    }
}
