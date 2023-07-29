package org.jholsten.me2e.mock.parser

import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.utils.MockServerStubValidator
import org.jholsten.me2e.parsing.YamlParser
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Class for parsing the stub definition defined in a YAML file.
 */
internal class YamlMockServerStubParser : MockServerStubParser, YamlParser<MockServerStub>(
    MockServerStubValidator(DeserializerFactory.getYamlMapper()), MockServerStub::class.java
)
