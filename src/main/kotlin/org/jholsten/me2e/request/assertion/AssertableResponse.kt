package org.jholsten.me2e.request.assertion

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jholsten.me2e.assertions.AssertionFailure
import org.jholsten.me2e.assertions.matchers.Assertable
import org.jholsten.me2e.request.model.HttpResponse
import java.util.regex.Pattern

/**
 * Model for asserting that the properties of the given [response] are as expected.
 */
class AssertableResponse internal constructor(private val response: HttpResponse) {

    /**
     * Asserts that the status code of the [response] satisfies the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun statusCode(expected: Assertable<Int?>) {
        expected.evaluate("status code", this.response.code)
    }

    /**
     * Asserts that the protocol of the [response] satisfies the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun protocol(expected: Assertable<String?>) {
        expected.evaluate("protocol", this.response.protocol)
    }

    /**
     * Asserts that the message of the [response] satisfies the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun message(expected: Assertable<String?>) {
        expected.evaluate("message", this.response.message)
    }

    /**
     * Asserts that the headers of the [response] satisfy the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun headers(expected: Assertable<Map<String, List<*>>?>) {
        expected.evaluate("headers", this.response.headers.entries)
    }

    /**
     * Asserts that the content type of the [response] satisfies the given assertion.
     * @throws AssertionFailure if content type is not set or if assertion was not successful.
     */
    fun contentType(expected: Assertable<String?>) {
        expected.evaluate("content type", this.response.body?.contentType?.value)
    }

    /**
     * Asserts that the body of the [response], encoded as string, satisfies the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun body(expected: Assertable<String?>) {
        expected.evaluate("body", this.response.body?.asString())
    }

    /**
     * Asserts that the body of the [response], encoded as byte array, satisfies the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun binaryBody(expected: Assertable<ByteArray?>) {
        expected.evaluate("binary body", this.response.body?.asBinary())
    }

    /**
     * Asserts that the body of the [response], encoded as base 64, satisfies the given assertion.
     * @throws AssertionFailure if assertion was not successful.
     */
    fun base64Body(expected: Assertable<String?>) {
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
     * assertThat(response).jsonBody("authors[0]", isEqualTo("{\"firstname":\"Vahid\",\"lastname\":\"Garousi\"}"))
     * assertThat(response).jsonBody("year", isEqualTo("2016"))
     * assertThat(response).jsonBody("keywords[1]", isEqualTo("TestAutomation"))
     * assertThat(response).jsonBody("journal.title", isEqualTo("IEEE Software"))
     * ```
     * @throws AssertionFailure if assertion was not successful.
     */
    fun jsonBody(key: String, expected: Assertable<String?>) {
        val json = this.response.body?.asJson() ?: throw AssertionFailure("Response does not contain a response body")
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
     * Asserts that the [response] conforms to the given specification.
     * @throws AssertionFailure if at least one assertion was not successful.
     */
    fun conformsTo(specification: ResponseSpecification) {
        specification.evaluate(this)
    }

    private fun findNode(root: JsonNode, key: String): JsonNode? {
        val arrayKeyMatcher = Pattern.compile("(.*)\\[(\\d)\\]").matcher(key)
        return if (arrayKeyMatcher.find()) {
            val strippedKey = arrayKeyMatcher.group(1)
            val arrayIndex = arrayKeyMatcher.group(2).toInt()
            findArrayNode(root, strippedKey, arrayIndex)
        } else {
            root.findByKey(key)
        }
    }

    private fun findArrayNode(root: JsonNode, key: String, arrayIndex: Int): JsonNode? {
        val node = root.findByKey(key) ?: return null
        if (!node.isArray) {
            throw AssertionFailure("Expected node $key to be an array, but is ${node.nodeType}.")
        }
        val arrayNode = node as ArrayNode
        return arrayNode.get(arrayIndex)
    }

    private fun JsonNode.findByKey(key: String): JsonNode? {
        for ((elementKey, element) in this.fields()) {
            if (elementKey == key) {
                return element
            }
        }
        return null
    }
}

// TODO: Define one Class Assertions with all methods, e.g.
/*
isEqualTo(expected: String): StringMatcher
isEqualTo(expected: Int): NumberMatcher
isEqualTo(expected: Any): ValueMatcher
 */

/*
backend.get("/search")
    .assertThat() // Or maybe: do not allow this and only enable the syntax below (as AssertJ)
        .statusCode(isEqualTo(200))
        .protocol(isEqualTo("http/1.1.")
        .headers(contains("key").withValue("abc"))
        .headers(contains("Content-Type").withValue("abc"))
        .contentType(isEqualTo("JSON"))
        .jsonBody("lastname", isEqualTo("Doe"))
        .body(isEqualTo("Some Response")
        .body(contains("file.txt"))

backend.get(RelativeUrl("/search")).assertThat().conformsTo(specification)

val response = backend.get("/search")

assertThat(response).statusCode(isEqualTo(200))
assertThat(response).conformsTo(specification)

val specification = ResponseSpecification.Builder()
    .expectStatusCode(isEqualTo(200))
    .expectBody(isEqualTo("Some Response"))
    .expectHeaders(contains("key").withValue("abc"))
    .build()
 */
