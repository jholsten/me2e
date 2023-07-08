package org.jholsten.me2e.config.exception

/**
 * Exception that indicates an error occurring on parsing the test configuration.
 */
class ConfigParseException(
    message: String,
): RuntimeException(message)
