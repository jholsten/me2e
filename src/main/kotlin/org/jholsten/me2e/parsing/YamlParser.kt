package org.jholsten.me2e.parsing

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import java.lang.Exception

/**
 * Generic service that parses YAML contents.
 */
internal class YamlParser : Parser {

    override fun <T> parse(value: String, clazz: Class<T>): T {
        try {
            return DeserializerFactory.getYamlMapper().readValue(value, clazz)
        } catch (e: MismatchedInputException) {
            throw ValidationException(listOf("${e.path.joinToString(".") { it.fieldName }}: ${e.message}"))
        } catch (e: Exception) {
            throw ParseException("Parsing value '$value' failed: ${e.message}")
        }
    }
}
