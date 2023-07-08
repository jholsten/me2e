package org.jholsten.me2e.config.exception

/**
 * Exception that occurs when a required field is missing in the test configuration.
 */
class MissingFieldException(
    message: String,
): ConfigParseException(message)
