package org.jholsten.me2e.request.assertion

/**
 * Exception that occurs when an assertion was not successful.
 */
class AssertionFailure(message: String, val failures: List<String> = listOf(message)) : AssertionError(message)
