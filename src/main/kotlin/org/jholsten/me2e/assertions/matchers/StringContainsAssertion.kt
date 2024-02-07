package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking if a string value contains an expected value.
 */
class StringContainsAssertion(expected: String) : Assertable<String?>(
    assertion = { actual -> actual?.contains(expected) ?: false },
    message = "to contain\n\t$expected",
)
