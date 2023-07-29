package org.jholsten.me2e.parsing.exception

/**
 * Exception that occurs when the test configuration is invalid.
 * This covers the following cases:
 *  - required fields are missing
 *  - value is not one of the enum values
 *  - value cannot be deserialized to the given type
 */
class ValidationException(
    val validationErrors: List<String>,
) : ParseException("Validation failed: $validationErrors")
