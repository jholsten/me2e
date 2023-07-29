package org.jholsten.me2e.mock.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.jholsten.me2e.parsing.utils.SchemaValidator

/**
 * Validator that verifies that the stub definition matches the JSON schema.
 * @param mapper Object mapper to use for reading values
 */
internal class MockServerStubValidator(mapper: ObjectMapper) : SchemaValidator("stub_schema.json", mapper)
