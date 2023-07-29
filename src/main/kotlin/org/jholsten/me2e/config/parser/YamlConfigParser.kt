package org.jholsten.me2e.config.parser

import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.utils.ConfigValidator
import org.jholsten.me2e.parsing.YamlParser
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils

/**
 * Class for parsing test configuration defined in YAML file.
 */
internal class YamlConfigParser : ConfigParser {
    override fun parseFile(filename: String): TestConfig {
        val fileContents = FileUtils.readFileContentsFromResources(filename)
        ConfigValidator.validate(fileContents, DeserializerFactory.getYamlMapper())

        return YamlParser().parse(fileContents, TestConfig::class.java)
    }
}
