package org.jholsten.me2e.parsing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.parsing.utils.SchemaValidator
import java.lang.Exception

/**
 * Generic service that parses YAML contents.
 */
internal open class YamlParser<T>(
    /**
     * Schema validator to use
     */
    private val validator: SchemaValidator,

    /**
     * Model to which value should be parsed
     */
    private val clazz: Class<T>,

    /**
     * Object mapper to use for deserialization.
     * By default, this is set to the standard YAML mapper defined in the [DeserializerFactory].
     * In case any additional configuration needs to be applied, this value can be set to a
     * custom mapper.
     */
    private val yamlMapper: ObjectMapper = DeserializerFactory.getYamlMapper(),
) : Parser<T> {

    override fun parse(value: String): T {
        validator.validate(value)

        try {
            return yamlMapper.readValue(value, clazz)
        } catch (e: MismatchedInputException) {
            val path = e.path.filter { it.fieldName != null }.joinToString(".") { it.fieldName }
            throw ValidationException(listOf("$path: ${e.message}"))
        } catch (e: Exception) {
            throw ParseException("Parsing value '$value' failed: ${e.message}")
        }
    }

    override fun parseFile(filename: String): T {
        val fileContents = FileUtils.readFileContentsFromResources(filename)
        return parse(fileContents)
    }
}
