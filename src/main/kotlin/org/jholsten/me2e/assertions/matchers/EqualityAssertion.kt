package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking the equality of values of type [T].
 */
class EqualityAssertion<T>(expected: T) : Assertable<T>(
    assertion = { actual -> expected == actual },
    message = { property, actual -> "Expected $property\n\t$actual\nto be equal to\n\t$expected" },
)
