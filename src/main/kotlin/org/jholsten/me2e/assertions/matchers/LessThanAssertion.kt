package org.jholsten.me2e.assertions.matchers


/**
 * Assertion for checking whether the actual value is less than the expected value.
 * @param expected Numeric value which should be greater than the actual value.
 * @param T Numeric datatype of the values to compare.
 */
class LessThanAssertion<T> internal constructor(private val expected: T) : Assertable<T?>(
    assertion = { actual -> actual?.let { actual < expected } == true },
    message = "to be less than\n\t$expected",
) where T : Number?, T : Comparable<T> {
    override fun toString(): String = "less than $expected"
}
