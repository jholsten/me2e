package org.jholsten.me2e.request.assertions

import com.fasterxml.jackson.databind.JsonNode
import org.jholsten.me2e.assertions.AssertionFailure
import org.jholsten.me2e.assertions.matchers.Assertable
import org.jholsten.me2e.assertions.matchers.JsonBodyAssertion
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Specification of an expected response.
 * For this specification, all expectations for the properties of an [org.jholsten.me2e.request.model.HttpResponse]
 * can be defined, which are then evaluated collectively. This enables to evaluate several assertions and obtain all
 * failed assertions and not just the first one. Furthermore, the specification can be reused across multiple tests.
 * @see AssertableResponse.conformsTo
 */
class ExpectedResponse {
    private val statusCode: MutableList<Assertable<Int?>> = mutableListOf()
    private val protocol: MutableList<Assertable<String?>> = mutableListOf()
    private val message: MutableList<Assertable<String?>> = mutableListOf()
    private val headers: MutableList<Assertable<Map<String, List<*>>?>> = mutableListOf()
    private val contentType: MutableList<Assertable<String?>> = mutableListOf()
    private val body: MutableList<Assertable<String?>> = mutableListOf()
    private val binaryBody: MutableList<Assertable<ByteArray?>> = mutableListOf()
    private val base64Body: MutableList<Assertable<String?>> = mutableListOf()
    private val jsonBody: MutableList<Assertable<JsonNode?>> = mutableListOf()

    /**
     * Expects that the status code of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the status code.
     *
     * Example:
     * ```kotlin
     * ExpectedResponse().expectStatusCode(equalTo(200))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.statusCode].
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
     * ExpectedResponse().expectProtocol(equalTo("HTTP/1.1"))
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
     * ExpectedResponse().expectMessage(equalTo("OK"))
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
     * ExpectedResponse().expectHeaders(containsKey("Content-Type").withValue(equalTo("application/json")))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.headers].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.headers
     */
    fun expectHeaders(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.headers.add(expected)
    }

    /**
     * Expects that the content type of the response body satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the content type.
     *
     * Example:
     * ```kotlin
     * ExpectedResponse().expectContentType(equalTo("application/json"))
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
     * ExpectedResponse().expectBody(equalTo("Text Content"))
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
     * ExpectedResponse().expectBinaryBody(equalTo(byteArrayOf(123, 34, 110)))
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
     * ExpectedResponse().expectBase64Body(equalTo("YWRtaW46c2VjcmV0"))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.base64Body
     */
    fun expectBase64Body(expected: Assertable<String?>) = apply {
        this.base64Body.add(expected)
    }

    /**
     * Expects that the body of the response, parsed as JSON, satisfies the given assertion.
     * See [JsonBodyAssertion] for detailed information on the format of the [JsonBodyAssertion.expectedPath].
     * You may call this function multiple times to place multiple requirements on the body.
     *
     * Example:
     * ```kotlin
     * ExpectedResponse().jsonBody(containsNode("journal.title").withValue(equalTo("IEEE Software")))
     * ```
     * @param expected Expectation for the value of the [HttpResponse.body].
     * @return This instance, to use for chaining.
     * @see AssertableResponse.jsonBody
     */
    fun expectJsonBody(expected: Assertable<JsonNode?>) = apply {
        this.jsonBody.add(expected)
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
            jsonBody.map { evaluate { response.jsonBody(it) } },
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
