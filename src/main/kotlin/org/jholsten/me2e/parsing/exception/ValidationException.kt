package org.jholsten.me2e.parsing.exception

/**
 * Exception that occurs when a value to parse is invalid.
 * This covers the following cases:
 *  - required fields are missing
 *  - value is not one of the enum values
 *  - value cannot be deserialized to the given type
 */
class ValidationException : ParseException {
    val validationErrors: List<String>

    internal constructor(validationErrors: List<String>) : super("Validation failed: $validationErrors") {
        this.validationErrors = validationErrors
    }

    internal constructor(message: String) : super(message) {
        this.validationErrors = listOf()
    }
}
