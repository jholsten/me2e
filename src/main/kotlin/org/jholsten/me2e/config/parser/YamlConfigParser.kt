package org.jholsten.me2e.config.parser

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.utils.ConfigValidator
import org.jholsten.me2e.parsing.YamlParser
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Class for parsing test configuration defined in YAML file.
 */
internal class YamlConfigParser : ConfigParser, YamlParser<TestConfig>(
    ConfigValidator(DeserializerFactory.getYamlMapper()), TestConfig::class.java
)
