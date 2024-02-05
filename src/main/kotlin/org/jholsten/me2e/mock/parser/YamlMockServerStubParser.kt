package org.jholsten.me2e.mock.parser

import com.fasterxml.jackson.databind.ObjectMapper
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.parsing.YamlParser
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Class for parsing the mock server stub definition defined in a YAML or JSON file.
 */
internal class YamlMockServerStubParser(
    /**
     * Object mapper to use for parsing the file contents.
     * Should be compatible with both YAML and JSON files.
     */
    yamlMapper: ObjectMapper,
) : MockServerStubParser, YamlParser<MockServerStub>(
    schemaValidator = MockServerStubSchemaValidator(DeserializerFactory.getYamlMapper()),
    clazz = MockServerStub::class.java,
    yamlMapper = yamlMapper
)
