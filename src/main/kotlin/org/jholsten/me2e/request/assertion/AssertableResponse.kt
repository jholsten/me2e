package org.jholsten.me2e.request.assertion

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jholsten.me2e.request.assertion.matchers.Assertable
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Model for asserting that the properties of the given [response] are as expected.
 */
class AssertableResponse internal constructor(private val response: HttpResponse) {

    /**
     * Asserts that the status code of the [response] satisfies the given assertion.
     * @param expected Expectation for the value of the [HttpResponse.code].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun statusCode(expected: Assertable<Int?>) = apply {
        expected.evaluate("status code", this.response.code)
    }

    /**
     * Asserts that the protocol of the [response] satisfies the given assertion.
     * @param expected Expectation for the value of the [HttpResponse.protocol].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun protocol(expected: Assertable<String?>) = apply {
        expected.evaluate("protocol", this.response.protocol)
    }

    /**
     * Asserts that the message of the [response] satisfies the given assertion.
     * @param expected Expectation for the value of the [HttpResponse.message].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun message(expected: Assertable<String?>) = apply {
        expected.evaluate("message", this.response.message)
    }

    /**
     * Asserts that the headers of the [response] satisfy the given assertion.
     * @param expected Expectation for the value of the [HttpResponse.headers].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun headers(expected: Assertable<Map<String, List<*>>?>) = apply {
        expected.evaluate("headers", this.response.headers.entries)
    }

    /**
     * Asserts that the content type of the [response] satisfies the given assertion.
     * @param expected Expectation for the value of the [org.jholsten.me2e.request.model.HttpResponseBody.contentType].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if content type is not set or if assertion was not successful.
     */
    fun contentType(expected: Assertable<String?>) = apply {
        expected.evaluate("content type", this.response.body?.contentType?.value)
    }

    /**
     * Asserts that the body of the [response], encoded as string, satisfies the given assertion.
     * @param expected Expectation for the string value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun body(expected: Assertable<String?>) = apply {
        expected.evaluate("body", this.response.body?.asString())
    }

    /**
     * Asserts that the body of the [response], encoded as byte array, satisfies the given assertion.
     * @param expected Expectation for the binary value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun binaryBody(expected: Assertable<ByteArray?>) = apply {
        expected.evaluate("binary body", this.response.body?.asBinary())
    }

    /**
     * Asserts that the body of the [response], encoded as base 64, satisfies the given assertion.
     * @param expected Expectation for the base 64 encoded value of the [HttpResponse.body].
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun base64Body(expected: Assertable<String?>) = apply {
        expected.evaluate("base 64 body", this.response.body?.asBase64())
    }

    /**
     * Asserts that the body of the [response], parsed as JSON, satisfies the given assertion for the element with the given key.
     * Use `.` as path separators to specify a path in the JSON tree. Use `[{index}]` to specify the element at index `index` in
     * an array node.
     *
     * Example:
     * Given is the following JSON body representing an article.
     * ```json
     * {
     *      "title": "Developing, Verifying, and Maintaining High-Quality Automated Test Scripts",
     *      "authors": [
     *          {
     *              "firstname": "Vahid",
     *              "lastname": "Garousi"
     *          },
     *          {
     *              "firstname": "Michael",
     *              "lastname": "Felderer"
     *          }
     *      ],
     *      "year": 2016,
     *      "keywords": ["Software Testing", "Test Automation"],
     *      "journal": {
     *          "title": "IEEE Software",
     *          "volume": 33,
     *          "issue": 3
     *      }
     * }
     * ```
     *
     * For this response body, following assertions do not throw an exception.
     * ```
     * assertThat(response).jsonBody("title", contains("Automated Test Scripts"))
     * assertThat(response).jsonBody("authors[0].lastname", isEqualTo("Garousi"))
     * assertThat(response).jsonBody("authors[0]", isEqualTo("{\"firstname\":\"Vahid\",\"lastname\":\"Garousi\"}"))
     * assertThat(response).jsonBody("year", isEqualTo("2016"))
     * assertThat(response).jsonBody("keywords[1]", isEqualTo("Test Automation"))
     * assertThat(response).jsonBody("journal.title", isEqualTo("IEEE Software"))
     * ```
     * @param key Key of the JSON body to evaluate.
     * @param expected Expectation for the value of the JSON node with the given key.
     * @return This instance, to use for chaining. Note that the following assertions will not be evaluated if this
     * assertion fails. To evaluate all assertions, use [conformsTo] in combination with [ResponseSpecification].
     * @throws AssertionFailure if assertion was not successful.
     */
    fun jsonBody(key: String, expected: Assertable<String?>) = apply {
        val json = this.response.body?.asJson() ?: throw AssertionFailure("Response does not contain a response body.")
        val path = key.split(".")
        var node: JsonNode = json
        for (subKey in path) {
            node = findNode(node, subKey) ?: throw AssertionFailure("Unable to find JSON node with key $subKey in path $key.")
        }
        val stringRepresentation = when {
            node.isValueNode -> node.asText()
            else -> node.toString()
        }
        expected.evaluate("json body at path $key", stringRepresentation)
    }

    /**
     * Asserts that the [response] conforms to the given specification. Evaluates assertions for all properties.
     * @param specification Expectation for the [response].
     * @throws AssertionFailure if at least one assertion was not successful.
     */
    fun conformsTo(specification: ResponseSpecification) {
        specification.evaluate(this)
    }

    /**
     * Tries to find the JSON node with the given key in the given root.
     * For keys specifying an element in an array, the key with format `property[{index}]` is destructured into
     * the name of the property and the index of the element. For regular properties, the JSON node with the
     * specified key is returned. If the [root] does not contain a JSON node with the specified property, `null`
     * is returned.
     * @param root JSON node to search for the property with the given key.
     * @param key Key of the JSON node to find. May be a property or an indexed property.
     */
    private fun findNode(root: JsonNode, key: String): JsonNode? {
        val arrayKeyMatcher = Regex("(.*)\\[(\\d)\\]").find(key)
        return if (arrayKeyMatcher != null) {
            val strippedKey = arrayKeyMatcher.groupValues[1]
            val arrayIndex = arrayKeyMatcher.groupValues[2].toInt()
            findArrayNode(root, strippedKey, arrayIndex)
        } else {
            root.findByKey(key)
        }
    }

    /**
     * Tries to find element in array node with the given key in the given root at the given index.
     * Returns `null` if the [root] does not contain a JSON node with the given key.
     * @param root JSON node to search for the element with the given key.
     * @param key Key of the JSON node to find.
     * @throws AssertionFailure if JSON node with the given key is not an array node.
     */
    private fun findArrayNode(root: JsonNode, key: String, arrayIndex: Int): JsonNode? {
        val node = root.findByKey(key) ?: return null
        if (!node.isArray) {
            throw AssertionFailure("Expected node $key to be an array, but is ${node.nodeType}.")
        }
        val arrayNode = node as ArrayNode
        return arrayNode.get(arrayIndex)
    }

    /**
     * Returns the JSON node with the given key or `null`, if such key does not exist.
     * @param key Key of the JSON node to find.
     */
    private fun JsonNode.findByKey(key: String): JsonNode? {
        for ((elementKey, element) in this.fields()) {
            if (elementKey == key) {
                return element
            }
        }
        return null
    }
}
