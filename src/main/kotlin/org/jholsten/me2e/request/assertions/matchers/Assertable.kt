package org.jholsten.me2e.request.assertions.matchers

import org.jholsten.me2e.request.assertions.AssertionFailure

/**
 * Base class for assertions. Enables to evaluate an assertion and throw a corresponding [AssertionFailure]
 * if the assertion was not successful.
 * @param T Datatype of the values to compare.
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
     * @param property Name of the property to be evaluated.
     * @param actual Actual value to be evaluated.
     * @throws AssertionFailure if assertion was not successful.
     */
    @JvmSynthetic
    internal fun evaluate(property: String, actual: T) {
        if (!assertion(actual)) {
            throw AssertionFailure(message(property, actual))
        }
    }
}
