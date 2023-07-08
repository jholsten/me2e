package org.jholsten.me2e.config.exception

/**
 * Exception that occurs when the format of the test configuration file is invalid.
 */
class InvalidFormatException(
    message: String,
): ConfigParseException(message)
