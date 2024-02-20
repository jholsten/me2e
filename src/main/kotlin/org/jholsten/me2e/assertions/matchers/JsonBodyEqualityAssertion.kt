package org.jholsten.me2e.assertions.matchers

import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import org.intellij.lang.annotations.Language

/**
 * Assertion for checking the equality of JSON objects.
 * Use [whenIgnoringNodes] to specify nodes of the objects which should be ignored.
 * By default, all values of all nodes are compared.
 */
class JsonBodyEqualityAssertion internal constructor(private val expected: JsonNode) : Assertable<JsonNode?>(
    assertion = { actual -> expected == actual },
    message = "to be equal to\n\t$expected",
) {

    /**
     * Specifies the paths of nodes to ignore when comparing the actual JSON object to the expected one.
     * The paths need to valid JSONPath specifications. For detailed information on the JSONPath syntax,
     * see [IETF](https://datatracker.ietf.org/doc/draft-ietf-jsonpath-base/).
     * @param nodesToIgnore Paths to the nodes to be ignored when comparing the JSON objects.
     */
    fun whenIgnoringNodes(@Language("JSONPath") vararg nodesToIgnore: String): Assertable<JsonNode?> {
        return whenIgnoringNodes(nodesToIgnore.toList())
    }

    /**
     * Returns assertion for comparing JSON nodes while ignoring the nodes at the given paths.
     * Internally, those nodes to ignore are removed from both the expected and the actual JSON object.
     */
    private fun whenIgnoringNodes(nodesToIgnore: List<String>): Assertable<JsonNode?> {
        return object : Assertable<JsonNode?>(
            assertion = { actual -> expected.removeNodes(nodesToIgnore) == actual?.removeNodes(nodesToIgnore) },
            message = "to be equal to\n\t$expected\nwhile ignoring nodes\n\t$nodesToIgnore",
        ) {
            override fun toString(): String = "equal to $expected ignoring nodes $nodesToIgnore"
        }
    }

    override fun toString(): String = "equal to $expected"

    /**
     * Extension function to remove nodes with the given paths from the given JSON node.
     * @return JSON node without the [nodesToIgnore].
     */
    private fun JsonNode.removeNodes(nodesToIgnore: List<String>): JsonNode {
        val copy = this.deepCopy<JsonNode>()
        val document = JsonPath.parse(copy, jsonPathConfig)
        for (nodeToIgnore in nodesToIgnore) {
            try {
                document.delete(nodeToIgnore)
            } catch (e: PathNotFoundException) {
                // Ignore unknown path
            } catch (e: InvalidPathException) {
                throw IllegalArgumentException("Invalid JSONPath: ${e.message}")
            } catch (_: Exception) {
                // Ignore exceptions
            }
        }
        return copy
    }

    companion object {
        /**
         * JSONPath configuration for handling objects of type [JsonNode].
         */
        private val jsonPathConfig = Configuration.defaultConfiguration().jsonProvider(JacksonJsonNodeJsonProvider())
    }
}
