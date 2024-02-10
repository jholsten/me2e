package org.jholsten.me2e.parsing.exception

/**
 * Exception that occurs when the format of the test configuration file is invalid.
 */
class InvalidFormatException internal constructor(message: String) : ParseException(message)
