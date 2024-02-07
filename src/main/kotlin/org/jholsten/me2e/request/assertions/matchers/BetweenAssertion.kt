package org.jholsten.me2e.request.assertions.matchers


/**
 * Assertion for checking whether the actual value is within a range of numeric values.
 * @param lowerBound Numeric value which should be less than or equal to the actual value.
 * @param upperBound Numeric value which should be greater than or equal to the actual value.
 * @param T Numeric datatype of the values to compare.
 */
class BetweenAssertion<T>(lowerBound: T, upperBound: T) : Assertable<T?>(
    assertion = { actual -> actual?.let { actual in lowerBound..upperBound } == true },
    message = { property, actual -> "Expected $property\n\t$actual\nto be in range\n\t$lowerBound <= x <= $upperBound" },
) where T : Number?, T : Comparable<T>
