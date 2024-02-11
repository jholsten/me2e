package org.jholsten.me2e.assertions.matchers

import org.jholsten.me2e.assertions.AssertionFailure

/**
 * Base class for assertions. Enables to evaluate an assertion and throw a corresponding [AssertionFailure]
 * if the assertion was not successful.
 * @constructor Instantiates a new [Assertable] instance.
 * @param assertion Assertion which should evaluate to `true`.
 * @param message Message representing the expectation to output in case the assertion was not successful.
 * @param T Datatype of the values to compare.
 */
open class Assertable<T> internal constructor(
    /**
     * Assertion which should evaluate to `true`.
     * Takes actual value as parameter and must return a boolean value.
     */
    val assertion: (T) -> Boolean,

    /**
     * Message representing the expectation to output in case assertion was not successful.
     * Is prefixed with `"Expected $property\n\t$actual\n"`.
     */
    val message: String,
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
            val message = "Expected $property\n\t$actual\n$message"
            throw AssertionFailure(message)
        }
    }
}
