package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking the equality of values of type [T].
 * @param T Datatype of the values to compare.
 */
class EqualityAssertion<T>(expected: T) : Assertable<T>(
    assertion = { actual -> expected == actual },
    message = "to be equal to\n\t$expected",
)
