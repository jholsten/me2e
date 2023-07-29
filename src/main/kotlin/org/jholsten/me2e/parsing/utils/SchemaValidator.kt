package org.jholsten.me2e.parsing.utils

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ValidationException
import kotlin.jvm.Throws

/**
 * Validator that verifies that a value matches a JSON schema.
 */
internal class SchemaValidator(
    /**
     * Filename of the JSON schema. Needs to be located in resources folder.
     */
    schema: String,

    /**
     * Object mapper to use for reading the value.
     */
    private val mapper: ObjectMapper,
) {
    /**
     * JSON schema to use for validating the value.
     */
    private val schema: JsonSchema

    init {
        this.schema = readJsonSchema(schema)
    }

    /**
     * Validates that the given value matches the specified JSON schema.
     * @param value Value to validate
     */
    @Throws(InvalidFormatException::class, ValidationException::class)
    fun validate(value: String) {
        val result = this.schema.validate(readValue(value))

        if (result.isEmpty()) {
            return
        }

        throwExceptionForValidationErrors(result)
    }

    private fun readJsonSchema(schema: String): JsonSchema {
        val factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).objectMapper(this.mapper).build()
        return factory.getSchema(FileUtils.getResourceAsStream(schema))
    }

    private fun readValue(value: String): JsonNode {
        try {
            return this.mapper.readTree(value)
        } catch (e: JsonParseException) {
            throw InvalidFormatException("Value could not be parsed: ${e.message}")
        }
    }

    private fun throwExceptionForValidationErrors(errors: Set<ValidationMessage>) {
        val typeErrors = errors.filter { it.type == "type" }

        if (typeErrors.isNotEmpty()) {
            throw InvalidFormatException("Invalid format: ${typeErrors.map { it.message }}")
        }

        throw ValidationException(errors.map { it.message })
    }
}
