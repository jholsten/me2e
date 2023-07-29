package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.utils.ConfigValidator
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import java.lang.Exception

/**
 * Class for parsing test configuration defined in YAML file.
 */
internal class YamlConfigParser : ConfigParser {
    override fun parseFile(filename: String): TestConfig {
        val fileContents = FileUtils.readFileContentsFromResources(filename)
        ConfigValidator.validate(fileContents, DeserializerFactory.getYamlMapper())

        try {
            return DeserializerFactory.getYamlMapper().readValue(fileContents, TestConfig::class.java)
        } catch (e: MismatchedInputException) {
            throw ValidationException(listOf("${e.path.joinToString(".") { it.fieldName }}: ${e.message}"))
        } catch (e: Exception) {
            throw ParseException("Parsing the YAML test configuration failed: ${e.message}")
        }
    }
}
