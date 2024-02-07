package org.jholsten.me2e.assertions.matchers


/**
 * Assertion for checking whether the actual value is greater than the expected value.
 * @param expected Numeric value which should be less than the actual value.
 * @param T Numeric datatype of the values to compare.
 */
class GreaterThanAssertion<T>(expected: T) : Assertable<T?>(
    assertion = { actual -> actual?.let { actual > expected } == true },
    message = { property, actual -> "Expected $property\n\t$actual\nto be greater than\n\t$expected" },
) where T : Number?, T : Comparable<T>
