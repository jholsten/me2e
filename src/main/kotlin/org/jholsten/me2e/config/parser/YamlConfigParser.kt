package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.jholsten.me2e.config.exception.InvalidFormatException
import org.jholsten.me2e.config.exception.MissingFieldException
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.utils.FileUtils

/**
 * Class for parsing test configuration defined in YAML file.
 */
class YamlConfigParser: ConfigParser {
    companion object {
        private val YAML_MAPPER = YAMLMapper().registerModule(KotlinModule.Builder().build())
    }
    
    override fun parseFile(filename: String): TestConfig {
        val fileContents = FileUtils.readFileFromResources(filename)
        // TODO: validate
        try {
            return YAML_MAPPER.readValue(fileContents, TestConfig::class.java)
        } catch (e: MissingKotlinParameterException) {
            throw MissingFieldException("")
        } catch (e: MismatchedInputException) {
            throw InvalidFormatException(e.message ?: "INVALID? TODO")
        } catch (e: DatabindException) {
            throw MissingFieldException(e.message ?: "INVALID?TODO")
        }
    
        return YAML_MAPPER.readValue(fileContents, TestConfig::class.java)
        
        //TODO("Not yet implemented")
    }
}
