package org.jholsten.me2e.config.parser

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.parsing.YamlParser
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Class for parsing test configuration defined in YAML file.
 */
internal class YamlConfigParser : ConfigParser, YamlParser<TestConfig>(
    schemaValidator = ConfigSchemaValidator(DeserializerFactory.getYamlMapper()),
    additionalValueValidators = listOf(ConfigValidator()),
    clazz = TestConfig::class.java
)
