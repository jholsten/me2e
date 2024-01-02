package org.jholsten.me2e.assertions

/**
 * Exception that occurs when an assertion was not successful.
 */
class AssertionFailure(message: String, val failures: List<String>) : AssertionError(message) {
    constructor(message: String) : this(message, failures = listOf(message))
}
