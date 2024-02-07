package org.jholsten.me2e.mock.verification

import com.fasterxml.jackson.databind.JsonNode
import com.github.tomakehurst.wiremock.http.Request
import org.jholsten.me2e.assertions.AssertionFailure
import org.jholsten.me2e.assertions.matchers.Assertable
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpMethod

/**
 * Model for verifying that a Mock Server received a certain request.
 * The starting point for these verifications are the methods [receivedRequest], which are used to specify how often
 * the subsequently defined request is expected. The methods of this class can then be used to specify the expected
 * request, for example by specifying the expected request method (see [withMethod]).
 * TODO: Add samples
 * TODO: Maybe replace string matcher with something else so that naming is similar to Response-Assertions?
 *
 * TODO: Desired interface:
 * assertThat(mockServer).receivedRequest().withPath(equalTo("/search")) NOT WORKING BECAUSE I NEED TO EVALUATE THIS ALL TOGETHER
 * assertThat(mockServer).receivedRequest(1, ExpectedRequest() // OR: ExpectedRequest() + ExpectedResponse()
 *      .withPath(equalTo("/search"))
 *      .withMessage(equalTo("Hello"))
 * )
 *
 * AssertableMockServerVerification(mockServer)
 *
 * assertThat(response).statusCode(equalTo(200))
 * assertThat(response).statusCode(notEqualTo(200))
 * assertThat(response).statusCode(lessThan(200))
 * assertThat(response).statusCode(between(200, 300))
 * TODO: toString
 */
class ExpectedRequest {
    private var stubName: String? = null
    private val method: MutableList<Assertable<HttpMethod?>> = mutableListOf()
    private val path: MutableList<Assertable<String?>> = mutableListOf()
    private val headers: MutableList<Assertable<Map<String, List<*>>?>> = mutableListOf()
    private val queryParameters: MutableList<Assertable<Map<String, List<*>>?>> = mutableListOf()
    private val contentType: MutableList<Assertable<String?>> = mutableListOf()
    private val body: MutableList<Assertable<String?>> = mutableListOf()
    private val binaryBody: MutableList<Assertable<ByteArray?>> = mutableListOf()
    private val base64Body: MutableList<Assertable<String?>> = mutableListOf()
    private val jsonBody: MutableList<Assertable<JsonNode?>> = mutableListOf()

    @JvmSynthetic
    internal var noOther: Boolean = false

    private var stub: MockServerStub? = null

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
    fun withMethod(expected: Assertable<HttpMethod?>) = apply {
        this.method.add(expected)
    }

    /**
     * Path that the expected incoming request should have.
     * Not setting this value means that the path of the expected request is arbitrary.
     * @param expected Pattern that the path of the actual request should conform to.
     * @return This instance, to use for chaining.
     */
    fun withPath(expected: Assertable<String?>) = apply {
        this.path.add(expected)
    }

    /**
     * Header that the expected incoming request should include.
     * Not setting this value means that the headers of the expected request are arbitrary.
     * @param key Case-insensitive key of the expected request header.
     * @param headerValue String matcher for the value of the expected request header.
     * @return This instance, to use for chaining.
     */
    fun withHeaders(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.headers.add(expected)
    }

    fun withContentType(expected: Assertable<String?>) = apply {
        this.contentType.add(expected)
    }

    /**
     * Query parameter that the expected incoming request should include.
     * Not setting this value means that the query parameters of the expected request are arbitrary.
     * @param key Case-insensitive key of the expected query parameter.
     * @param queryParameterValue String matcher for the value of the expected query parameter.
     * @return This instance, to use for chaining.
     */
    fun withQueryParameters(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.queryParameters.add(expected)
    }

    /**
     * Request body that the expected incoming request should have. In order to place several requirements
     * on the expected request body, use the chaining methods [StringMatcher.and] and [StringMatcher.or].
     * Not setting this value means that the request body of the expected request is arbitrary.
     * @param bodyMatcher Pattern that the body of the actual request should conform to.
     * @return This instance, to use for chaining.
     */
    fun withBody(expected: Assertable<String?>) = apply {
        this.body.add(expected)
    }

    fun withBinaryBody(expected: Assertable<ByteArray?>) = apply {
        this.binaryBody.add(expected)
    }

    fun withBase64Body(expected: Assertable<String?>) = apply {
        this.base64Body.add(expected)
    }

    fun withJsonBoy(expected: Assertable<JsonNode?>) = apply {
        this.jsonBody.add(expected)
    }

    // TODO: JSON Body

    /**
     * Expect that the Mock Server only received this specified request and no other.
     * @return This instance, to use for chaining.
     */
    fun andNoOther() = apply {
        this.noOther = true
    }

    @JvmSynthetic
    internal fun matches(mockServer: MockServer, request: Request): Boolean {
        if (this.stubName != null) {
            this.stub = mockServer.stubs.firstOrNull { it.name == this.stubName }
            requireNotNull(this.stub) { "No stub with name ${this.stubName} exists for Mock Server ${mockServer.name}." }
            return this.stub!!.request.matches(request)
        }
        val mappedRequest = HttpRequestMapper.INSTANCE.toInternalDto(request)
        var json: JsonNode? = null
        if (this.jsonBody.isNotEmpty()) {
            json = try {
                mappedRequest.body?.asJson()
            } catch (e: Exception) {
                throw AssertionFailure("Unable to parse body as JSON: ${e.message}")
            }
        }
        val results = listOf(
            listOf(mockServer.hostname == request.host),
            method.map { evaluate { it.evaluate("method", mappedRequest.method) } },
            path.map { evaluate { it.evaluate("path", mappedRequest.url.path) } },
            headers.map { evaluate { it.evaluate("headers", mappedRequest.headers.entries) } },
            queryParameters.map { evaluate { it.evaluate("path", mappedRequest.url.queryParameters) } },
            body.map { evaluate { it.evaluate("body", mappedRequest.body?.asString()) } },
            binaryBody.map { evaluate { it.evaluate("binary body", mappedRequest.body?.asBinary()) } },
            base64Body.map { evaluate { it.evaluate("base 64 body", mappedRequest.body?.asBase64()) } },
            jsonBody.map { evaluate { it.evaluate("json body", json) } },
        ).flatten()
        return results.all { it }
    }

    /**
     * Evaluates the given assertion. Returns `null` if the assertion was successful.
     * Returns the message of the failure in case the assertion was not successful.
     * @return `null` if assertion was successful, else the message of the failure.
     */
    private fun evaluate(assertion: () -> Unit): Boolean {
        return try {
            assertion()
            true
        } catch (e: AssertionFailure) {
            false
        }
    }

    override fun toString(): String {
        if (this.stubName != null) {
            return this.stub?.toString() ?: this.stubName!!
        }
        return "TODO (Probably easier when only using one object and not a list)"
    }
}
