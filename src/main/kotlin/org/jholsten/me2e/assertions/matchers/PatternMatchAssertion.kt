package org.jholsten.me2e.assertions.matchers

import org.intellij.lang.annotations.Language

/**
 * Assertion for checking if a string value matches a regex pattern.
 */
class PatternMatchAssertion internal constructor(@Language("RegExp") private val expected: String) : Assertable<String?>(
    assertion = { actual -> actual?.let { expected.toRegex().matches(it) } ?: false },
    message = "to match pattern\n\t$expected",
) {
    override fun toString(): String = "matches pattern $expected"
}
