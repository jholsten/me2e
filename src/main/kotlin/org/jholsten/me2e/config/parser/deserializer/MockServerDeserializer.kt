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
 * Custom deserializer for parsing Mock Servers defined in the `mock-servers` section of the config file
 * to [MockServer] instances. Includes parsing stub files to [MockServerStub] instances.
 *
 * Prior to executing this deserializer, the schema has already been validated using the
 * [org.jholsten.me2e.config.parser.ConfigSchemaValidator], so that it can be assumed that all required
 * fields are set for all Mock Server definitions. In addition, the `name` field was set for each Mock
 * Server to its key in the [TestEnvironmentConfigDeserializer].
 */
internal class MockServerDeserializer : JsonDeserializer<MockServer>() {
    companion object {
        /**
         * Name of the injectable field which contains the `hostname` of the Mock Server
         * defined in the [MockServer.hostname]. Can be injected in other objects using
         * [com.fasterxml.jackson.annotation.JacksonInject].
         */
        @JvmSynthetic
        internal const val INJECTABLE_HOSTNAME_FIELD_NAME = "hostname"
    }

    /**
     * Object mapper to use for deserializing Mock Server stubs.
     */
    private val mapper = DeserializerFactory.getYamlMapper()

    /**
     * Deserializes one Mock Server definition to a [MockServer] instance.
     * Parses referenced stub definition files to instances of [MockServerStub].
     * @return Deserialized Mock Server instance.
     */
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
     * Sets injectable field with name [INJECTABLE_HOSTNAME_FIELD_NAME] to value of [hostname] to be able to
     * inject this value in the [org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher.hostname].
     * @return List of deserialized Mock Server stub definitions.
     */
    private fun parseStubFiles(hostname: String, stubsNode: JsonNode?): List<MockServerStub> {
        if (stubsNode == null || !stubsNode.isArray) {
            return listOf()
        }

        val stubFiles = stubsNode.elements().asSequence().map { it.textValue() }
        mapper.setInjectableValues(InjectableValues.Std().addValue(INJECTABLE_HOSTNAME_FIELD_NAME, hostname))
        val stubParser = YamlMockServerStubParser(mapper)
        return stubFiles.map { stubParser.parseFile(it) }.toList()
    }
}
