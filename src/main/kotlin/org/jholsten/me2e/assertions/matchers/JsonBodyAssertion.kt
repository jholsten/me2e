package org.jholsten.me2e.assertions.matchers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import org.jholsten.me2e.assertions.AssertionFailure

/**
 * Assertion for checking whether a JSON body contains a node with the given path.
 * Use `.` as path separators to specify a path in the JSON tree. Use `[{index}]` to specify the element at index
 * `index` in an array node. For assertions concerning the value of the node with the [expectedPath], use [withValue].
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
 * For this response body, the following assertions do not throw an exception.
 * ```
 * assertThat(response).jsonBody(containsNode("title").withValue(containsString("Automated Test Scripts")))
 * assertThat(response).jsonBody(containsNode("authors[0].lastname").withValue(equalTo("Garousi")))
 * assertThat(response).jsonBody(containsNode("authors[0]").withValue(equalTo("{\"firstname\":\"Vahid\",\"lastname\":\"Garousi\"}")))
 * assertThat(response).jsonBody(containsNode("year").withValue(equalTo("2016")))
 * assertThat(response).jsonBody(containsNode("keywords[1]").withValue(equalTo("Test Automation")))
 * assertThat(response).jsonBody(containsNode("journal.title").withValue(equalTo("IEEE Software")))
 * ```
 */
class JsonBodyAssertion(private val expectedPath: String) : Assertable<JsonNode?>(
    assertion = { actual -> actual != null && findNodeByPath(actual, expectedPath) != null },
    message = "to contain node with path\n\t$expectedPath",
) {
    /**
     * Returns assertion for checking if the JSON body contains a node with the expected path with an expected value.
     * @param expectedValue Expectation for the value of the JSON node with the given key.
     */
    fun withValue(expectedValue: Assertable<String?>): Assertable<JsonNode?> {
        return object : Assertable<JsonNode?>(
            assertion = { actual ->
                actual != null && findNodeByPath(actual, expectedPath)?.let { evaluateValue(it, expectedValue) } == true
            },
            message = "to contain node with key $expectedPath with value ${expectedValue.message}"
        ) {
            override fun toString(): String = "contains node with path $expectedPath and value $expectedValue"
        }
    }

    /**
     * Returns whether the value of the given JSON node, encoded as string, satisfies the given assertion.
     * @param node JSON node for which the value is to be evaluated.
     * @param expected Expectation for the value of the given JSON node.
     */
    private fun evaluateValue(node: JsonNode, expected: Assertable<String?>): Boolean {
        val stringRepresentation = when {
            node.isValueNode -> node.asText()
            else -> node.toString()
        }
        return expected.assertion(stringRepresentation)
    }

    companion object {
        /**
         * Tries to find the JSON node with the given path in the given root.
         * Returns `null` if node with the given path could not be found.
         * @param root JSON body to search for the node with the given path.
         * @param path Path to the JSON node to find.
         */
        private fun findNodeByPath(root: JsonNode, path: String): JsonNode? {
            val subKeys = path.split(".")
            var node: JsonNode = root
            for (subKey in subKeys) {
                node = findNode(node, subKey) ?: return null
            }
            return node
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

    override fun toString(): String = "contains node with path $expectedPath"
}
