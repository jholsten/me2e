package org.jholsten.me2e.assertions

/**
 * Exception that occurs when an assertion was not successful.
 */
class AssertionFailure internal constructor(
    /**
     * Message describing the assertion failure.
     */
    message: String,

    /**
     * List of messages for each failed assertion.
     * In case only one assertion was evaluated, this list contains one
     * entry which is equal to the [message].
     */
    val failures: List<String> = listOf(message)
) : AssertionError(message)
