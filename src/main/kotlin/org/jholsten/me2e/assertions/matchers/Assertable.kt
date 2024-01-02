package org.jholsten.me2e.assertions.matchers

import org.jholsten.me2e.assertions.AssertionFailure

/**
 * Base class for assertions.
 * @param T Datatype of the actual value which is compared to an expected value.
 */
open class Assertable<T> internal constructor(
    /**
     * Assertion which should evaluate to `true`.
     * Takes actual value as parameter and must return a boolean value.
     */
    private val assertion: (T) -> Boolean,

    /**
     * Message to output in case assertion was not successful.
     * Takes property name and actual value as parameters and must return the message.
     */
    private val message: (String, T) -> String,
) {
    /**
     * Evaluates the assertion for the given actual value.
     * @throws AssertionFailure if assertion was not successful.
     */
    internal fun evaluate(property: String, actual: T) {
        if (!assertion(actual)) {
            throw AssertionFailure(message(property, actual))
        }
    }
}
