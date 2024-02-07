package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking if a string value matches a regex pattern.
 */
class PatternMatchAssertion(expected: String) : Assertable<String?>(
    assertion = { actual -> actual?.let { expected.toRegex().matches(it) } ?: false },
    message = "to match pattern\n\t$expected",
)
