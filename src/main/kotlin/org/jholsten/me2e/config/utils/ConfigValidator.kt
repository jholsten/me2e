package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import org.jholsten.me2e.config.exception.InvalidFormatException
import org.jholsten.me2e.config.exception.ValidationException
import kotlin.jvm.Throws

/**
 * Validator that verifies that the configuration matches the JSON schema.
 */
class ConfigValidator {
    
    companion object {
        /**
         * Validates that the given value matches the JSON schema for the test configuration.
         * @param value Test configuration value to validate
         * @param mapper Object mapper to use for reading the value
         */
        @Throws(InvalidFormatException::class, ValidationException::class)
        fun validate(value: String, mapper: ObjectMapper) {
            val schema = readJsonSchema(mapper)
            val result = schema.validate(mapper.readTree(value))
            
            if (result.isEmpty()) {
                return
            }
            
            throwExceptionForValidationErrors(result)
        }
        
        private fun readJsonSchema(mapper: ObjectMapper): JsonSchema {
            val factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).objectMapper(mapper).build()
            return factory.getSchema(FileUtils.getResourceAsStream("config_schema.json"))
        }
        
        private fun throwExceptionForValidationErrors(errors: Set<ValidationMessage>) {
            val typeErrors = errors.filter { it.type == "type" }
            
            if (typeErrors.isNotEmpty()) {
                throw InvalidFormatException("Invalid test configuration format: ${typeErrors.map { it.message }}")
            }
            
            throw ValidationException(errors.map { it.message })
        }
    }
}
