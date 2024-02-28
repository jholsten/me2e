package org.jholsten.me2e.assertions.matchers

import com.fasterxml.jackson.databind.JsonNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.InvalidPathException
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import org.intellij.lang.annotations.Language
import org.jholsten.me2e.assertions.AssertionFailure
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.JSONCompareResult

/**
 * Assertion for checking the equality of JSON objects.
 * Use [ignoringNodes] to specify nodes of the objects which should be ignored.
 * By default, all values of all nodes are compared.
 */
class JsonBodyEqualityAssertion internal constructor(private val expected: JsonNode) : Assertable<JsonNode?>(
    assertion = { actual -> expected == actual },
    message = "to be equal to\n\n${expected.toPrettyString()}",
) {

    override fun evaluate(property: String, actual: JsonNode?) {
        evaluate(property, expected, actual, message)
    }

    /**
     * Specifies the paths of nodes to ignore when comparing the actual JSON object to the expected one.
     * The paths need to valid JSONPath specifications. For detailed information on the JSONPath syntax,
     * see [IETF](https://datatracker.ietf.org/doc/draft-ietf-jsonpath-base/).
     * @param nodesToIgnore Paths to the nodes to be ignored when comparing the JSON objects.
     */
    fun ignoringNodes(@Language("JSONPath") vararg nodesToIgnore: String): Assertable<JsonNode?> {
        return ignoringNodes(nodesToIgnore.toList())
    }

    /**
     * Returns assertion for comparing JSON nodes while ignoring the nodes at the given paths.
     * Internally, those nodes to ignore are removed from both the expected and the actual JSON object.
     */
    private fun ignoringNodes(nodesToIgnore: List<String>): Assertable<JsonNode?> {
        return object : Assertable<JsonNode?>(
            assertion = { actual -> expected.removeNodes(nodesToIgnore) == actual?.removeNodes(nodesToIgnore) },
            message = "to be equal to\n\n${expected.toPrettyString()}\n\nwhile ignoring nodes\n\t$nodesToIgnore",
        ) {
            override fun evaluate(property: String, actual: JsonNode?) {
                evaluate(property, expected.removeNodes(nodesToIgnore), actual?.removeNodes(nodesToIgnore), message)
            }

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

    /**
     * Evaluates differences between the given [expected] and [actual] JSON objects.
     * Includes differences in the message of the [AssertionFailure].
     */
    private fun evaluate(property: String, expected: JsonNode, actual: JsonNode?, message: String) {
        if (actual == null) {
            throw AssertionFailure("Expected $property\n\tnull\n$message")
        }
        val diff = JSONCompare.compareJSON(expected.toString(), actual.toString(), JSONCompareMode.STRICT)
        if (diff.failed()) {
            val diffMessage = diff.buildMessage()
            val failures = diff.fieldFailures.size + diff.fieldUnexpected.size + diff.fieldMissing.size
            throw AssertionFailure("Expected $property\n${actual.toPrettyString()}\n\n$message\n\nFound $failures Failures:\n$diffMessage")
        }
    }

    /**
     * Builds message representing the differences of the two JSON objects contained in the given result.
     */
    private fun JSONCompareResult.buildMessage(): String {
        val stringBuilder = StringBuilder()
        if (this.fieldMissing.isNotEmpty()) {
            stringBuilder.appendLine(">>> Missing Fields:")
            for (field in this.fieldMissing) {
                stringBuilder.appendLine("\t- ${if (!field.field.isNullOrBlank()) "${field.field}." else ""}${field.expected}")
            }
            stringBuilder.appendLine()
        }
        if (this.fieldUnexpected.isNotEmpty()) {
            stringBuilder.appendLine(">>> Unexpected Fields:")
            for (field in this.fieldUnexpected) {
                stringBuilder.appendLine("\t- ${if (!field.field.isNullOrBlank()) "${field.field}." else ""}${field.actual}")
            }
            stringBuilder.appendLine()
        }
        if (this.fieldFailures.isNotEmpty()) {
            stringBuilder.appendLine(">>> Mismatched values:")
            for (field in this.fieldFailures) {
                stringBuilder.appendLine("\t- Expected field \"${field.field}\" to have value\n\t\t${field.expected}\n\t  but was\n\t\t${field.actual}\n")
            }
            stringBuilder.appendLine()
        }
        return stringBuilder.toString()
    }

    companion object {
        /**
         * JSONPath configuration for handling objects of type [JsonNode].
         */
        private val jsonPathConfig = Configuration.defaultConfiguration().jsonProvider(JacksonJsonNodeJsonProvider())
    }
}
