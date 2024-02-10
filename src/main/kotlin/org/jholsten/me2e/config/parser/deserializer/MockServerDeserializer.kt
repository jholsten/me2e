package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.parser.YamlMockServerStubParser
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Custom deserializer that parses stub files to [MockServerStub] instances.
 */
internal class MockServerDeserializer : JsonDeserializer<MockServer>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MockServer {
        val node = p.readValueAsTree<ObjectNode>()
        val fields = node.fields().asSequence().map { it.key to it.value }.toMap()
        val (name, hostname) = fields["name"]?.textValue()!! to fields["hostname"]?.textValue()!!
        return MockServer(
            name = name,
            hostname = hostname,
            stubs = parseStubFiles(hostname, fields["stubs"]),
        )
    }

    /**
     * Parses stub files defined in node [stubsNode] to [MockServerStub] instances.
     * Sets injectable field `hostname` to value of [hostname].
     */
    private fun parseStubFiles(hostname: String, stubsNode: JsonNode?): List<MockServerStub> {
        if (stubsNode == null || !stubsNode.isArray) {
            return listOf()
        }

        val stubFiles = stubsNode.elements().asSequence().map { it.textValue() }
        val mapper = DeserializerFactory.getYamlMapper().setInjectableValues(InjectableValues.Std().addValue("hostname", hostname))
        val stubParser = YamlMockServerStubParser(mapper)
        return stubFiles.map { stubParser.parseFile(it) }.toList()
    }
}
