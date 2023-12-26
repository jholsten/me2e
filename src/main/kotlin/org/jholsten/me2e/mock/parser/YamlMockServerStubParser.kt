package org.jholsten.me2e.mock.parser

import com.fasterxml.jackson.databind.ObjectMapper
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.utils.MockServerStubSchemaValidator
import org.jholsten.me2e.parsing.YamlParser
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Class for parsing the stub definition defined in a YAML file.
 */
internal class YamlMockServerStubParser(
    yamlMapper: ObjectMapper,
) : MockServerStubParser, YamlParser<MockServerStub>(
    schemaValidator = MockServerStubSchemaValidator(DeserializerFactory.getYamlMapper()),
    clazz = MockServerStub::class.java,
    yamlMapper = yamlMapper
)
