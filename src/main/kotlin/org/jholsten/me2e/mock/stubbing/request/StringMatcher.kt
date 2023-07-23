package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Pattern to match string values.
 */
class StringMatcher(
    /**
     * Matches only if the string is exactly equal to the defined one.
     */
    internal val equals: String? = null,

    /**
     * Matches if the string conforms to the defined regex pattern.
     */
    internal val matches: String? = null,

    /**
     * Matches if the string does not conform to the defined regex pattern.
     */
    @JsonProperty("not-matches")
    internal val notMatches: String? = null,

    /**
     * Matches if the string contains the defined one.
     */
    internal val contains: String? = null,

    /**
     * Matches if the string does not contain the defined one.
     */
    @JsonProperty("not-contains")
    internal val notContains: String? = null,

    /**
     * Whether to disable case sensitivity for the string matching.
     */
    @JsonProperty("ignore-case")
    internal val ignoreCase: Boolean,
) {

    /**
     * Returns whether the given string conforms to all of the requirements of this matcher.
     */
    internal fun matches(value: String): Boolean {
        if (!matchesEqual(value)) {
            return false
        } else if (!matchesPattern(value)) {
            return false
        } else if (!matchesContains(value)) {
            return false
        }

        return true
    }

    internal fun matchesEqual(value: String): Boolean {
        if (this.equals == null) {
            return true
        }

        return this.equals.equals(value, this.ignoreCase)
    }

    internal fun matchesPattern(value: String): Boolean {
        val regexOptions = when (this.ignoreCase) {
            true -> setOf(RegexOption.IGNORE_CASE)
            else -> setOf()
        }

        if (this.matches != null && !this.matches.toRegex(regexOptions).matches(value)) {
            return false
        }

        if (this.notMatches != null && this.notMatches.toRegex(regexOptions).matches(value)) {
            return false
        }

        return true
    }

    internal fun matchesContains(value: String): Boolean {
        if (this.contains != null && !value.contains(this.contains, this.ignoreCase)) {
            return false
        }

        if (this.notContains != null && value.contains(this.notContains, this.ignoreCase)) {
            return false
        }

        return true
    }
}
