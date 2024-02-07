package org.jholsten.me2e.request.assertions

import org.jholsten.me2e.request.assertions.matchers.Assertable
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Specification of an expected response.
 * For this specification, all expectations of the properties of an [org.jholsten.me2e.request.model.HttpResponse]
 * can be defined, which are then evaluated collectively. This enables to evaluate several assertions and obtain
 * all failed ones and not just the first. Furthermore, the specification can be reused across multiple tests.
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
     */
    fun expectStatusCode(expected: Assertable<Int?>) = apply {
        this.statusCode.add(expected)
    }

    /**
     * Expects that the protocol of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the protocol.
     * @param expected Expectation for the value of the [HttpResponse.protocol].
     * @return This instance, to use for chaining.
     */
    fun expectProtocol(expected: Assertable<String?>) = apply {
        this.protocol.add(expected)
    }

    /**
     * Expects that the status message of the response satisfies the given assertion.
     * You may call this function multiple times to place multiple requirements on the status message.
     * @param expected Expectation for the value of the [HttpResponse.message].
     * @return This instance, to use for chaining.
     */
    fun expectMessage(expected: Assertable<String?>) = apply {
        this.message.add(expected)
    }

    /**
     * Expects that the headers of the response satisfy the given assertion.
     * You may call this function multiple times to place multiple requirements on the headers.
     * @param expected Expectation for the value of the [HttpResponse.headers].
     * @return This instance, to use for chaining.
     */
    fun expectHeaders(expected: Assertable<Map<String, List<*>>?>) = apply {
        this.headers.add(expected)
    }

    fun expectContentType(expected: Assertable<String?>) = apply {
        this.contentType.add(expected)
    }

    fun expectBody(expected: Assertable<String?>) = apply {
        this.body.add(expected)
    }

    fun expectBinaryBody(expected: Assertable<ByteArray?>) = apply {
        this.binaryBody.add(expected)
    }

    fun expectBase64Body(expected: Assertable<String?>) = apply {
        this.base64Body.add(expected)
    }

    fun expectJsonBody(key: String, expected: Assertable<String?>) = apply {
        this.jsonBody.add(key to expected)
    }

    /**
     * Evaluates whether all expected values match the actual response.
     * @param response Actual response to be evaluated.
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

    private fun evaluate(evaluation: () -> Unit): String? {
        return try {
            evaluation()
            null
        } catch (e: AssertionFailure) {
            e.message
        }
    }
}
