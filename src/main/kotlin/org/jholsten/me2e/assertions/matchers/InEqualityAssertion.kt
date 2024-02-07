package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking the inequality of values of type [T].
 * @param T Datatype of the values to compare.
 */
class InEqualityAssertion<T>(expected: T) : Assertable<T>(
    assertion = { actual -> expected != actual },
    message = { property, actual -> "Expected $property\n\t$actual\nto not be equal to\n\t$expected" },
)
