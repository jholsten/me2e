package org.jholsten.me2e.assertions.matchers

import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Assertion for checking whether a JSON body contains a node with the given JSONPath expression.
 * Each expression needs to start with the root element `$`. Starting from this root, specify the path to the node
 * using `.` as path separators. Use `[{index}]`to specify the element at index `index` in an array node.
 * For more information on the JSONPath syntax, see [IETF](https://datatracker.ietf.org/doc/draft-ietf-jsonpath-base/).
 *
 * For assertions concerning the value of the node with the [expectedPath], use [withValue].
 *
 * Example:<br/>
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
 * assertThat(response).jsonBody(containsNode("$.title").withValue(containsString("Automated Test Scripts")))
 * assertThat(response).jsonBody(containsNode("$.authors[0].lastname").withValue(equalTo("Garousi")))
 * assertThat(response).jsonBody(containsNode("$.authors[0]").withValue(equalTo("{\"firstname\":\"Vahid\",\"lastname\":\"Garousi\"}")))
 * assertThat(response).jsonBody(containsNode("$.year").withValue(equalTo("2016")))
 * assertThat(response).jsonBody(containsNode("$.keywords[1]").withValue(equalTo("Test Automation")))
 * assertThat(response).jsonBody(containsNode("$.journal.title").withValue(equalTo("IEEE Software")))
 * ```
 * @param expectedPath Path of the node to compare in JSONPath notation.
 * @see <a href="https://datatracker.ietf.org/doc/draft-ietf-jsonpath-base/">IETF</a>
 */
class JsonNodeAssertion internal constructor(private val expectedPath: String) : Assertable<JsonNode?>(
    assertion = { actual -> actual != null && findNodeValue(actual, expectedPath) != null },
    message = "to contain node with path\n\t$expectedPath",
    stringRepresentation = { actual -> actual?.toPrettyString() },
) {
    /**
     * Returns assertion for checking if the JSON body contains a node with the expected path with an expected value.
     * Note that the value of the corresponding node is read as a string value.
     * @param expectedValue Expectation for the value of the JSON node with the given key.
     */
    fun withValue(expectedValue: Assertable<String?>): Assertable<JsonNode?> {

        return object : Assertable<JsonNode?>(
            assertion = { actual ->
                actual != null && findNodeValue(actual, expectedPath)?.let { expectedValue.assertion(it) } == true
            },
            message = "to contain node with key $expectedPath with value ${expectedValue.message}",
            stringRepresentation = { actual -> actual?.toPrettyString() },
        ) {
            override fun toString(): String = "contains node with path $expectedPath and value $expectedValue"
        }
    }

    companion object {
        /**
         * Tries to find the JSON node with the given path in the given root.
         * Returns `null` if node with the given path could not be found.
         * @param root JSON body to search for the node with the given path.
         * @param path Path to the JSON node to find.
         */
        private fun findNodeValue(root: JsonNode, path: String): String? {
            val document = JsonPath.parse(root.toString())
            return try {
                when (val value = document.read<Any?>(path)) {
                    null -> null
                    is String -> value
                    is Map<*, *> -> DeserializerFactory.getObjectMapper().writeValueAsString(value)
                    else -> value.toString()
                }
            } catch (e: PathNotFoundException) {
                null
            } catch (e: InvalidPathException) {
                throw IllegalArgumentException("Invalid JSONPath: ${e.message}")
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun toString(): String = "contains node with path $expectedPath"
}
