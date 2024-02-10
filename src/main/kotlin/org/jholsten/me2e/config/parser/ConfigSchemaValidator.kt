package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.databind.ObjectMapper
import org.jholsten.me2e.parsing.utils.SchemaValidator

/**
 * Validator that verifies that the configuration matches the JSON schema.
 * @param mapper Object mapper to use for reading values
 */
internal class ConfigSchemaValidator(mapper: ObjectMapper) : SchemaValidator("config_schema.json", mapper)
