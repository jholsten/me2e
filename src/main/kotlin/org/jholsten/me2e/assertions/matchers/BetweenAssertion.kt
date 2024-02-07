package org.jholsten.me2e.assertions.matchers


/**
 * Assertion for checking whether the actual value is within a range of numeric values.
 * @param lowerBound Numeric value which should be less than or equal to the actual value.
 * @param upperBound Numeric value which should be greater than or equal to the actual value.
 * @param T Numeric datatype of the values to compare.
 */
class BetweenAssertion<T>(private val lowerBound: T, private val upperBound: T) : Assertable<T?>(
    assertion = { actual -> actual?.let { actual in lowerBound..upperBound } == true },
    message = "to be in range\n\t$lowerBound <= value <= $upperBound",
) where T : Number?, T : Comparable<T> {
    override fun toString(): String = "in range $lowerBound <= value <= $upperBound"
}
