package org.jholsten.me2e.mock.stubbing.request

/**
 * Pattern to match multiple [StringMatcher] instances by combining them with a logical `AND`.
 * Only matches if both [matcher1] and [matcher2] match a given value.
 */
class LogicalAnd internal constructor(
    private val matcher1: StringMatcher,
    private val matcher2: StringMatcher,
) : StringMatcher() {
    override fun matches(value: String): Boolean {
        return matcher1.matches(value) && matcher2.matches(value)
    }
}
