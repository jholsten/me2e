package org.jholsten.me2e.request.assertions

import org.jholsten.me2e.request.assertions.matchers.Assertable
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Specification of an expected response.
 * For this specification, all expectations of the properties of an [org.jholsten.me2e.request.model.HttpResponse]
 * can be defined, which are then evaluated collectively. This enables to evaluate several assertions and obtain all
 * failed assertions and not just the first one. Furthermore, the specification can be reused across multiple tests.
 * @see AssertableResponse.conformsTo
 */
class ResponseSpecification {
    private var statusCode: MutableList<Assertable<Int?>> = mutableListOf()
    private var protocol: MutableList<Assertable<String?>> = mutableListOf()
    private var message: MutableList<Assertable<String?>> = mutableListOf()
    private var headers: MutableList<Assertable<Map<String, List<*>>?>> = mutableListOf()
    private var contentType: MutableList<Assertable<String?>> = mutableListOf()
    private var body: MutableList<Assertable<String?>> = mutableListOf()
    private var binaryBody: MutableList<Assertable<ByteArray?>> = mutableListOf()
    private var base64Body: MutableList<Assertable<String?>> = mutableListOf()
    private var jsonBody: MutableList<Pair<String, Assertable<String?>>> = mutableListOf()

    /**
     * Expects that the status code of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the status code.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectStatusCode(isEqualTo(200))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.code].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.statusCode
     */
    fun expectStatusCode(expected: Assertable<Int?>) = apply {
        this.statusCode.add(expected)
    }

    /**
     * Expects that the protocol of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the protocol.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectProtocol(isEqualTo("HTTP/1.1"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.protocol].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.protocol
     */
    fun expectProtocol(expected: Assertable<String?>) = apply {
        this.protocol.add(expected)
    }

    /**
     * Expects that the status message of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the status message.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectMessage(isEqualTo("OK"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.message].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.message
     */
    fun expectMessage(expected: Assertable<String?>) = apply {
        this.message.add(expected)
    }

    /**
     * Expects that the headers of the response satisfy the given assertion.
     * You may call this function multiple times to place multiple requirements on the headers.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectHeaders(containsKey("Content-Type").withValue("application/json"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.headers].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.headers
     */
    fun expectHeaders(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.headers.add(expected)
    }

    /**
     * Expects that the content type of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the content type.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectContentType(isEqualTo("application/json"))
     * ```
     * @param expected Expectation for the value of the [org.jholsten.me2e.request.model.HttpResponseBody.contentType].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.contentType
     */
    fun expectContentType(expected: Assertable<String?>) = apply {
        this.contentType.add(expected)
    }

    /**
     * Expects that the body of the response, encoded as string, satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the body.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectBody(isEqualTo("Text Content"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.body
     */
    fun expectBody(expected: Assertable<String?>) = apply {
        this.body.add(expected)
    }

    /**
     * Expects that the body of the response, encoded as byte array, satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the body.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectBinaryBody(isEqualTo(byteArrayOf(123, 34, 110)))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.binaryBody
     */
    fun expectBinaryBody(expected: Assertable<ByteArray?>) = apply {
        this.binaryBody.add(expected)
    }

    /**
     * Expects that the body of the response, encoded as base 64, satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the body.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectBase64Body(isEqualTo("YWRtaW46c2VjcmV0"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.base64Body
     */
    fun expectBase64Body(expected: Assertable<String?>) = apply {
        this.base64Body.add(expected)
    }

    /**
     * Expects that the body of the response, encoded as JSON, satisfies the given assertion for the element
     * with the given key. See [AssertableResponse.jsonBody] for detailed information on the format of the [key].
     * You may call this function multiple times to place multiple requirements on the body.
     *
     * Example:
     * ```kotlin
     * ResponseSpecification().expectJsonBody("journal.title", isEqualTo("IEEE Software"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.jsonBody
     */
    fun expectJsonBody(key: String, expected: Assertable<String?>) = apply {
        this.jsonBody.add(key to expected)
    }

    /**
     * Evaluates whether all expected values match the actual response.
     * In case at least one of the assertions was not successful, an [AssertionFailure] is thrown with all
     * failed assertions mentioned in the message.
     * @param response Actual response to be evaluated.
     * @throws AssertionFailure if at least one of the assertions was not successful.
     */
    @JvmSynthetic
    internal fun evaluate(response: AssertableResponse) {
        val messages = listOf(
            statusCode.map { evaluate { response.statusCode(it) } },
            protocol.map { evaluate { response.protocol(it) } },
            message.map { evaluate { response.message(it) } },
            headers.map { evaluate { response.headers(it) } },
            contentType.map { evaluate { response.contentType(it) } },
            body.map { evaluate { response.body(it) } },
            binaryBody.map { evaluate { response.binaryBody(it) } },
            base64Body.map { evaluate { response.base64Body(it) } },
            jsonBody.map { evaluate { response.jsonBody(it.first, it.second) } },
        ).flatten().filterNotNull()

        if (messages.isNotEmpty()) {
            val stringBuilder = StringBuilder()
            stringBuilder.appendLine("Response does not conform to expected specification. Found ${messages.size} assertion failures:")
            for (message in messages) {
                stringBuilder.appendLine("- $message")
            }
            throw AssertionFailure(stringBuilder.toString(), messages)
        }
    }

    /**
     * Evaluates the given assertion. Returns `null` if the assertion was successful.
     * Returns the message of the failure in case the assertion was not successful.
     * @return `null` if assertion was successful, else the message of the failure.
     */
    private fun evaluate(assertion: () -> Unit): String? {
        return try {
            assertion()
            null
        } catch (e: AssertionFailure) {
            e.message
        }
    }
}
