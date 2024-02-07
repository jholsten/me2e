package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking the inequality of values of type [T].
 * @param T Datatype of the values to compare.
 */
class InEqualityAssertion<T>(private val expected: T) : Assertable<T>(
    assertion = { actual -> expected != actual },
    message = "to not be equal to\n\t$expected",
) {
    override fun toString(): String = "unequal to $expected"
}
