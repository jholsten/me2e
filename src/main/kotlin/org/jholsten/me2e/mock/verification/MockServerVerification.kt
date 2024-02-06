package org.jholsten.me2e.mock.verification

import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.request.model.HttpMethod

/**
 * Model for verifying that a Mock Server received a certain request.
 * The starting point for these verifications are the methods [receivedRequest], which are used to specify how often
 * the subsequently defined request is expected. The methods of this class can then be used to specify the expected
 * request, for example by specifying the expected request method (see [withMethod]).
 */
class MockServerVerification private constructor(
    /**
     * Number of times that the Mock Server should have received this request.
     * If set to `null`, it is verified that the Mock Server received the specified request at least once.
     */
    internal val times: Int?,
) {
    companion object {
        /**
         * Entrypoint for specifying a request that a Mock Server should have received at least once.
         */
        @JvmStatic
        fun receivedRequest(): MockServerVerification {
            return MockServerVerification(null)
        }

        /**
         * Entrypoint for specifying a request that a Mock Server should have received exactly [times] number of times.
         * @param times Number of times that the Mock Server should have received this request.
         */
        @JvmStatic
        fun receivedRequest(times: Int): MockServerVerification {
            return MockServerVerification(times)
        }
    }

    @JvmSynthetic
    internal var stubName: String? = null

    @JvmSynthetic
    internal var method: HttpMethod? = null

    @JvmSynthetic
    internal var path: StringMatcher? = null

    @JvmSynthetic
    internal var headers: MutableMap<String, StringMatcher>? = null

    @JvmSynthetic
    internal var queryParameters: MutableMap<String, StringMatcher>? = null

    @JvmSynthetic
    internal var requestBodyPattern: StringMatcher? = null

    @JvmSynthetic
    internal var noOther: Boolean = false

    /**
     * Matches if the request pattern defined for the stub with the given [MockServerStub.name] matches the incoming request.
     * This can be used as a shortcut to setting the values of the expected stub.
     * @param stubName Name of the stub whose request pattern should match the actual request.
     * @return This instance, to use for chaining.
     */
    fun matchingStub(stubName: String) = apply {
        this.stubName = stubName
    }

    /**
     * HTTP method that the expected incoming request should have.
     * Not setting this value means that the HTTP method of the expected request is arbitrary.
     * @param method HTTP request method that the actual request should have.
     * @return This instance, to use for chaining.
     */
    fun withMethod(method: HttpMethod): MockServerVerification = apply {
        this.method = method
    }

    /**
     * Path that the expected incoming request should have.
     * Not setting this value means that the path of the expected request is arbitrary.
     * @param path Pattern that the path of the actual request should conform to.
     * @return This instance, to use for chaining.
     */
    fun withPath(path: StringMatcher): MockServerVerification = apply {
        this.path = path
    }

    /**
     * Header that the expected incoming request should include.
     * Not setting this value means that the headers of the expected request are arbitrary.
     * @param key Case-insensitive key of the expected request header.
     * @param headerValue String matcher for the value of the expected request header.
     * @return This instance, to use for chaining.
     */
    fun withHeader(key: String, headerValue: StringMatcher) = apply {
        if (this.headers == null) {
            this.headers = mutableMapOf()
        }
        this.headers!![key.lowercase()] = headerValue
    }

    /**
     * Query parameter that the expected incoming request should include.
     * Not setting this value means that the query parameters of the expected request are arbitrary.
     * @param key Case-insensitive key of the expected query parameter.
     * @param queryParameterValue String matcher for the value of the expected query parameter.
     * @return This instance, to use for chaining.
     */
    fun withQueryParameter(key: String, queryParameterValue: StringMatcher) = apply {
        if (this.queryParameters == null) {
            this.queryParameters = mutableMapOf()
        }
        this.queryParameters!![key.lowercase()] = queryParameterValue
    }

    /**
     * Request body that the expected incoming request should have. In order to place several requirements
     * on the expected request body, use the chaining methods [StringMatcher.and] and [StringMatcher.or].
     * Not setting this value means that the request body of the expected request is arbitrary.
     * @param bodyMatcher Pattern that the body of the actual request should conform to.
     * @return This instance, to use for chaining.
     */
    fun withRequestBody(bodyMatcher: StringMatcher) = apply {
        this.requestBodyPattern = bodyMatcher
    }

    /**
     * Expect that the Mock Server only received this specified request and no other.
     * @return This instance, to use for chaining.
     */
    fun andNoOther() = apply {
        this.noOther = true
    }
}
