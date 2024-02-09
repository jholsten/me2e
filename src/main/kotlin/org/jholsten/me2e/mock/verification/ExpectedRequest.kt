package org.jholsten.me2e.mock.verification

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.tomakehurst.wiremock.http.Request
import org.jholsten.me2e.assertions.AssertionFailure
import org.jholsten.me2e.assertions.matchers.Assertable
import org.jholsten.me2e.assertions.matchers.JsonBodyAssertion
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.request.mapper.HttpRequestMapper
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequest
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.utils.toJson

/**
 * Specification of an expected response that a Mock Server should have received.
 * For this specification, all expectations for the properties of an [org.jholsten.me2e.request.model.HttpRequest]
 * can be defined, which are then collectively compared to the actual requests that the Mock Server has received.
 * @see MockServerVerification.receivedRequest
 */
class ExpectedRequest {
    private var stubName: String? = null
    private val method: MutableList<Assertable<HttpMethod?>> = mutableListOf()
    private val path: MutableList<Assertable<String?>> = mutableListOf()
    private val headers: MutableList<Assertable<Map<String, List<*>>?>> = mutableListOf()
    private val queryParameters: MutableList<Assertable<Map<String, List<*>>?>> = mutableListOf()
    private val contentType: MutableList<Assertable<String?>> = mutableListOf()
    private val body: MutableList<Assertable<String?>> = mutableListOf()
    private val objectBody: MutableList<ObjectBodyAssertion<*>> = mutableListOf()
    private val binaryBody: MutableList<Assertable<ByteArray?>> = mutableListOf()
    private val base64Body: MutableList<Assertable<String?>> = mutableListOf()
    private val jsonBody: MutableList<Assertable<JsonNode?>> = mutableListOf()

    @JvmSynthetic
    internal var noOther: Boolean = false
    private var stub: MockServerStub? = null

    /**
     * Matches if the request pattern defined for the stub with the given [MockServerStub.name] matches the incoming request.
     * This can be used as a shortcut to setting the values of the expected request.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().matchingStub("stub-name"))
     * ```
     * @param stubName Name of the stub whose request pattern should match the actual request.
     * @return This instance, to use for chaining.
     */
    fun matchingStub(stubName: String) = apply {
        this.stubName = stubName
    }

    /**
     * Expects that the HTTP method of the expected incoming request satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the HTTP method.
     * Not setting an expectation means that the HTTP method of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withMethod(equalTo(HttpMethod.POST)))
     * ```
     * @param expected Expectation for the value of the [HttpRequest.method].
     * @return This instance, to use for chaining.
     */
    fun withMethod(expected: Assertable<HttpMethod?>) = apply {
        this.method.add(expected)
    }

    /**
     * Expects that the path of the URL of the expected incoming request satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the path of the URL.
     * Not setting an expectation means that the path of the URL of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withPath(equalTo("/search")))
     * ```
     * @param expected Expectation for the value of the [org.jholsten.me2e.request.model.Url.path].
     * @return This instance, to use for chaining.
     */
    fun withPath(expected: Assertable<String?>) = apply {
        this.path.add(expected)
    }

    /**
     * Expects that the headers of the expected incoming request satisfy the given assertion.
     * You may call this function multiple times to place multiple requirements on the headers.
     * Not setting an expectation means that the headers of the expected request are arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withHeaders(containsKey("Content-Type").withValue(equalTo("application/json"))))
     * ```
     * @param expected Expectation for the value of the [HttpRequest.headers].
     * @return This instance, to use for chaining.
     */
    fun withHeaders(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.headers.add(expected)
    }

    /**
     * Expects that the content type of the request body of the expected incoming request satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the content type.
     * Not setting an expectation means that the content type of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withContentType(equalTo("application/json")))
     * ```
     * @param expected Expectation for the value of the [org.jholsten.me2e.request.model.HttpRequestBody.contentType].
     * @return This instance, to use for chaining.
     */
    fun withContentType(expected: Assertable<String?>) = apply {
        this.contentType.add(expected)
    }

    /**
     * Expects that the query parameters of the expected incoming request satisfy the given assertion.
     * You may call this function multiple times to place multiple requirements on the query parameters.
     * Not setting an expectation means that the query parameters of the expected request are arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withQueryParameters(containsKey("id").withValue(equalTo("abc"))))
     * ```
     * @param expected Expectation for the value of the [org.jholsten.me2e.request.model.Url.queryParameters].
     * @return This instance, to use for chaining.
     */
    fun withQueryParameters(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.queryParameters.add(expected)
    }

    /**
     * Expects that the body of the expected incoming request, encoded as string, satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the request body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withBody(equalTo("Text Content")))
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @return This instance, to use for chaining.
     */
    fun withBody(expected: Assertable<String?>) = apply {
        this.body.add(expected)
    }

    /**
     * Expects that the body of the expected incoming request, deserialized to [type], satisfies the given assertion.
     * For Kotlin, it is recommended to use the inline function [withObjectBody] instead.
     * You may call this function multiple times to place multiple requirements on the body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```java
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withObjectBody(MyClass.class, equalTo(obj)));
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @param type Type to which the request body content should be deserialized.
     * @return This instance, to use for chaining.
     */
    fun <T> withObjectBody(type: Class<T>, expected: Assertable<T?>) = apply {
        this.objectBody.add(ObjectBodyAssertion(type = type, expected = expected))
    }

    /**
     * Expects that the body of the expected incoming request, deserialized to [type], satisfies the given assertion.
     * In Java, this is useful for deserializing lists of objects, for example.
     * For Kotlin, it is recommended to use the inline function [withObjectBody] instead.
     * You may call this function multiple times to place multiple requirements on the body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```java
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withObjectBody(new TypeReference<List<MyClass>>(){}, equalTo(list)));
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @param type Type to which the request body content should be deserialized.
     * @return This instance, to use for chaining.
     */
    fun <T> withObjectBody(type: TypeReference<T>, expected: Assertable<T?>) = apply {
        this.objectBody.add(ObjectBodyAssertion(typeReference = type, expected = expected))
    }

    /**
     * Expects that the body of the expected incoming request, deserialized to type [T], satisfies the given assertion.
     * Only available for Kotlin. You may call this function multiple times to place multiple requirements
     * on the body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```java
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withObjectBody<MyClass>(equalTo(list)));
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @param T Type to which the request body content should be deserialized.
     * @return This instance, to use for chaining.
     */
    inline fun <reified T> withObjectBody(expected: Assertable<T?>) = apply {
        withObjectBody(object : TypeReference<T>() {}, expected)
    }

    /**
     * Expects that the body of the expected incoming request, encoded as byte array, satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the request body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withBinaryBody(byteArrayOf(123, 34, 110)))
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @return This instance, to use for chaining.
     */
    fun withBinaryBody(expected: Assertable<ByteArray?>) = apply {
        this.binaryBody.add(expected)
    }

    /**
     * Expects that the body of the expected incoming request, encoded as base 64, satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the request body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().withBase64Body(equalTo("YWRtaW46c2VjcmV0")))
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @return This instance, to use for chaining.
     */
    fun withBase64Body(expected: Assertable<String?>) = apply {
        this.base64Body.add(expected)
    }

    /**
     * Expects that the body of the expected incoming request, parsed as JSON, satisfies the given assertion.
     * See [JsonBodyAssertion] for detailed information on the format of the [JsonBodyAssertion.expectedPath].
     * You may call this function multiple times to place multiple requirements on the request body.
     * Not setting an expectation means that the body of the expected request is arbitrary.
     *
     * Example:
     * ```kotlin
     * assertThat(mockServer).receivedRequest(ExpectedRequest().jsonBody(containsNode("journal.title").withValue(equalTo("IEEE Software"))))
     * ```
     * @param expected Expectation for the value of the [HttpRequest.body].
     * @return This instance, to use for chaining.
     */
    fun withJsonBody(expected: Assertable<JsonNode?>) = apply {
        this.jsonBody.add(expected)
    }

    /**
     * Expect that the Mock Server only received this specified request and no other.
     * @return This instance, to use for chaining.
     */
    fun andNoOther() = apply {
        this.noOther = true
    }

    /**
     * Returns whether the given request that the Mock Server [mockServer] received matches this expected request.
     * In case a named stub was defined via [matchingStub], it is evaluated whether the actual request matches the
     * pattern specified for the stub with the supplied name.
     */
    @JvmSynthetic
    internal fun matches(mockServer: MockServer, request: Request): Boolean {
        if (this.stubName != null) {
            this.stub = mockServer.stubs.firstOrNull { it.name == this.stubName }
            requireNotNull(this.stub) { "No stub with name ${this.stubName} exists for Mock Server ${mockServer.name}." }
            return this.stub!!.request.matches(request)
        }
        val mappedRequest = HttpRequestMapper.INSTANCE.toInternalDto(request)
        val json = getJson(mappedRequest)
        val results = listOf(
            listOf(mockServer.hostname == request.host),
            method.map { it.assertion(mappedRequest.method) },
            path.map { it.assertion(mappedRequest.url.path) },
            headers.map { it.assertion(mappedRequest.headers.entries) },
            queryParameters.map { it.assertion(mappedRequest.url.queryParameters) },
            body.map { it.assertion(mappedRequest.body?.asString()) },
            objectBody.map { it.evaluateBody(mappedRequest.body) },
            binaryBody.map { it.assertion(mappedRequest.body?.asBinary()) },
            base64Body.map { it.assertion(mappedRequest.body?.asBase64()) },
            jsonBody.map { it.assertion(json) },
        ).flatten()
        return results.all { it }
    }

    /**
     * Returns the request body of the given request, parsed as JSON, if at least one expectation for the JSON body was set.
     * @throws AssertionFailure if body could not be parsed.
     */
    private fun getJson(mappedRequest: HttpRequest): JsonNode? {
        if (this.jsonBody.isNotEmpty()) {
            return try {
                mappedRequest.body?.asJson()
            } catch (e: Exception) {
                throw AssertionFailure("Unable to parse body as JSON: ${e.message}")
            }
        }
        return null
    }

    override fun toString(): String {
        if (this.stubName != null) {
            return this.stub?.request?.toString() ?: this.stubName!!
        }

        val json = JsonNodeFactory.instance.objectNode()
        this.method.toJson()?.let { json.set<ObjectNode>("method", it) }
        this.path.toJson()?.let { json.set<ObjectNode>("path", it) }
        this.headers.toJson()?.let { json.set<ObjectNode>("headers", it) }
        this.queryParameters.toJson()?.let { json.set<ObjectNode>("query-parameters", it) }
        this.contentType.toJson()?.let { json.set<ObjectNode>("content-type", it) }
        this.body.toJson()?.let { json.set<ObjectNode>("body", it) }
        this.binaryBody.toJson()?.let { json.set<ObjectNode>("binary-body", it) }
        this.base64Body.toJson()?.let { json.set<ObjectNode>("base64-body", it) }
        this.jsonBody.toJson()?.let { json.set<ObjectNode>("json-body", it) }
        return toJson(json)
    }

    /**
     * Creates array node with entries of the given list, if it is not empty.
     * Is used to represent this expected request as JSON.
     */
    private fun List<Assertable<*>>.toJson(): JsonNode? {
        if (this.isEmpty()) {
            return null
        }
        val node = JsonNodeFactory.instance.arrayNode()
        for (assertable in this) {
            node.add(assertable.toString())
        }
        return node
    }

    /**
     * Wrapper for assertions concerning the object body deserialized to a given type.
     */
    private inner class ObjectBodyAssertion<T>(
        val type: Class<T>? = null,
        val typeReference: TypeReference<T>? = null,
        val expected: Assertable<T?>,
    ) {
        /**
         *
         * Evaluates the given assertion on the given request body.
         */
        fun evaluateBody(body: HttpRequestBody?): Boolean {
            val obj = try {
                deserializeBody(body)
            } catch (e: ParseException) {
                throw AssertionFailure("Unable to deserialize body: ${e.message}")
            }
            return expected.assertion(obj)
        }

        /**
         * Deserializes the given request body to an instance of type [T].
         * @throws ParseException if content could not be deserialized.
         */
        private fun deserializeBody(body: HttpRequestBody?): T? {
            return if (type != null) {
                body?.asObject(type)
            } else {
                body?.asObject(typeReference!!)
            }
        }
    }
}
