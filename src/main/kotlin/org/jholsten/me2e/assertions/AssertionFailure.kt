package org.jholsten.me2e.assertions

/**
 * Exception that occurs when an assertion was not successful.
 */
class AssertionFailure internal constructor(message: String, val failures: List<String> = listOf(message)) : AssertionError(message)
