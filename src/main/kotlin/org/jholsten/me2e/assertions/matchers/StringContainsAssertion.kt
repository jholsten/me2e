package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking if a string value contains an expected value.
 */
class StringContainsAssertion(private val expected: String) : Assertable<String?>(
    assertion = { actual -> actual?.contains(expected) ?: false },
    message = "to contain\n\t$expected",
) {
    override fun toString(): String = "contains $expected"
}
