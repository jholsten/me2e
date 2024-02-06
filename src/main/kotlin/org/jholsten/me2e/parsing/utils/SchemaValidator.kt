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

/**
 * Validator that verifies that a value conforms to a JSON schema.
 */
internal open class SchemaValidator(
    /**
     * Filename of the JSON schema. Needs to be located in `resources` folder.
     */
    schema: String,

    /**
     * Object mapper to use for reading the value.
     */
    private val mapper: ObjectMapper,
) : Validator<String> {
    /**
     * JSON schema to use for validating the value.
     */
    private val schema: JsonSchema = readJsonSchema(schema)

    /**
     * Validates that the given value conforms to the specified JSON schema.
     * @param value Value to validate.
     * @throws InvalidFormatException if value could not be parsed using the [mapper].
     * @throws ValidationException if value violates at least one of the requirements of the JSON schema.
     */
    override fun validate(value: String) {
        val result = this.schema.validate(readValue(value))

        if (result.isEmpty()) {
            return
        }

        throwExceptionForValidationErrors(result)
    }

    /**
     * Deserializes JSON schema located at the given path in `resources` folder.
     * @param schema Path to the schema to deserialize.
     * @return Deserialized [JsonSchema] instance.
     */
    private fun readJsonSchema(schema: String): JsonSchema {
        val factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).objectMapper(this.mapper).build()
        return factory.getSchema(FileUtils.getResourceAsStream(schema))
    }

    /**
     * Deserializes the given value using the [mapper] to a [JsonNode].
     * @param value Value to deserialize.
     * @return Value deserialized to a [JsonNode].
     */
    private fun readValue(value: String): JsonNode {
        try {
            return this.mapper.readTree(value)
        } catch (e: JsonParseException) {
            throw InvalidFormatException("Value could not be parsed: ${e.message}")
        }
    }

    /**
     * Throws [InvalidFormatException] or [ValidationException] for the given validation errors.
     * In case of type errors (e.g. invalid YAML format or invalid data types), an [InvalidFormatException]
     * is thrown. For errors describing the violation of the schema constraints (e.g. missing required fields,
     * invalid enum values), a [ValidationException] is thrown.
     * @param errors Validation errors as a result of the JSON schema validation.
     * @throws InvalidFormatException if [errors] contains at least one type error.
     * @throws ValidationException for violations of the schema constraints.
     */
    private fun throwExceptionForValidationErrors(errors: Set<ValidationMessage>) {
        val typeErrors = errors.filter { it.type == "type" }

        if (typeErrors.isNotEmpty()) {
            throw InvalidFormatException("Invalid format: ${typeErrors.map { it.message }}")
        }

        throw ValidationException(errors.map { it.message })
    }
}
