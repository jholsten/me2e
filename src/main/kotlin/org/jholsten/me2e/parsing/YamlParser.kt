package org.jholsten.me2e.parsing

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.parsing.utils.SchemaValidator
import org.jholsten.me2e.parsing.utils.Validator
import java.lang.Exception

/**
 * Generic service that parses YAML contents to instances of type [T].
 * @param T Model to which values should be parsed.
 */
internal open class YamlParser<T>(
    /**
     * JSON Schema validator to use for validating the value.
     */
    private val schemaValidator: SchemaValidator,

    /**
     * Additional validators to validate values of type [T].
     */
    private val additionalValueValidators: List<Validator<T>> = listOf(),

    /**
     * Model to which value should be parsed.
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
        schemaValidator.validate(value)

        val result = try {
            yamlMapper.readValue(value, clazz)
        } catch (e: MismatchedInputException) {
            val path = e.path.filter { it.fieldName != null }.joinToString(".") { it.fieldName }
            throw ValidationException(listOf("$path: ${e.message}"))
        } catch (e: ValidationException) {
            throw e
        } catch (e: Exception) {
            throw ParseException("Parsing value '$value' failed: ${e.message}")
        }

        for (validator in additionalValueValidators) {
            validator.validate(result)
        }
        return result
    }

    override fun parseFile(filename: String): T {
        val fileContents = FileUtils.readFileContentsFromResources(filename)
        return parse(fileContents)
    }
}
