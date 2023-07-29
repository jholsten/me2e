package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.parsing.utils.SchemaValidator
import kotlin.jvm.Throws

/**
 * Validator that verifies that the configuration matches the JSON schema.
 */
internal class ConfigValidator private constructor() {

    companion object {
        /**
         * Validates that the given value matches the JSON schema for the test configuration.
         * @param value Test configuration value to validate
         * @param mapper Object mapper to use for reading the value
         */
        @Throws(InvalidFormatException::class, ValidationException::class)
        @JvmStatic
        fun validate(value: String, mapper: ObjectMapper) {
            SchemaValidator("config_schema.json", mapper).validate(value)
        }
    }
}
