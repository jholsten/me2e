package org.jholsten.me2e.assertions.matchers

import org.jholsten.me2e.assertions.matchers.StringMatcher

/**
 * Pattern to match multiple [StringMatcher] instances by combining them with a logical `OR`.
 * Matches if [matcher1] and/or [matcher2] match a given value.
 */
class LogicalOr internal constructor(
    private val matcher1: StringMatcher,
    private val matcher2: StringMatcher,
) : StringMatcher() {
    override fun matches(value: String): Boolean {
        return matcher1.matches(value) || matcher2.matches(value)
    }
}
