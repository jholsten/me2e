package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.utils.toJson

/**
 * Pattern to match string values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class StringMatcher(
    /**
     * Matches only if the string is exactly equal to the defined one.
     */
    val equals: String? = null,

    /**
     * Matches if the string conforms to the defined regex pattern.
     */
    val matches: String? = null,

    /**
     * Matches if the string does not conform to the defined regex pattern.
     */
    @JsonProperty("not-matches")
    val notMatches: String? = null,

    /**
     * Matches if the string contains the defined one.
     */
    val contains: String? = null,

    /**
     * Matches if the string does not contain the defined one.
     */
    @JsonProperty("not-contains")
    val notContains: String? = null,

    /**
     * Whether to disable case sensitivity for the string matching.
     */
    @JsonProperty("ignore-case")
    val ignoreCase: Boolean = false,
) {
    companion object {
        /**
         * Returns matcher that matches if the String value to be compared is exactly
         * equal to the given [value].
         */
        @JvmStatic
        fun equalTo(value: String): StringMatcher {
            return StringMatcher(equals = value)
        }

        /**
         * Returns matcher that matches if the String value to be compared is equal
         * to the given [value] while ignoring case sensitivity.
         */
        @JvmStatic
        fun equalToIgnoreCase(value: String): StringMatcher {
            return StringMatcher(equals = value, ignoreCase = true)
        }

        /**
         * Returns matcher that matches if the String value to be compared conforms
         * to the given [regexPattern].
         */
        @JvmStatic
        fun matching(regexPattern: String): StringMatcher {
            return StringMatcher(matches = regexPattern)
        }

        /**
         * Returns matcher that matches if the String value to be compared does not
         * conform to the given [regexPattern].
         */
        @JvmStatic
        fun notMatching(regexPattern: String): StringMatcher {
            return StringMatcher(notMatches = regexPattern)
        }

        /**
         * Returns matcher that matches if the String value to be compared contains
         * exactly the given [value].
         */
        @JvmStatic
        fun containing(value: String): StringMatcher {
            return StringMatcher(contains = value)
        }

        /**
         * Returns matcher that matches if the String value to be compared contains
         * the given [value] while ignoring case sensitivity.
         */
        @JvmStatic
        fun containingIgnoreCase(value: String): StringMatcher {
            return StringMatcher(contains = value, ignoreCase = true)
        }

        /**
         * Returns matcher that matches if the String value to be compared does not
         * contain exactly the given [value].
         */
        @JvmStatic
        fun notContaining(value: String): StringMatcher {
            return StringMatcher(notContains = value)
        }
    }

    /**
     * Chains this matcher with the given other [matcher] using a logical `AND`.
     * Matches only if both this and [matcher] match a given value.
     */
    fun and(matcher: StringMatcher): LogicalAnd {
        return LogicalAnd(this, matcher)
    }

    /**
     * Chains this matcher with the given other [matcher] using a logical `OR`.
     * Matches if this and/or [matcher] match a given value.
     */
    fun or(matcher: StringMatcher): LogicalOr {
        return LogicalOr(this, matcher)
    }

    /**
     * Returns whether the given string conforms to all of the requirements of this matcher.
     */
    internal open fun matches(value: String): Boolean {
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

    override fun toString(): String = toJson(this)
}
