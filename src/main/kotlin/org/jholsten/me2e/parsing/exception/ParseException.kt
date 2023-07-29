package org.jholsten.me2e.parsing.exception

/**
 * Exception that indicates an error occurring on parsing.
 */
open class ParseException(
    message: String,
) : RuntimeException(message)
