package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.utils.toJson

/**
 * Pattern to match string values.
 * This matcher is used to verify the properties of the requests that should have been sent to a mock server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class StringMatcher private constructor(
    /**
     * Matches only if the string is exactly equal to the defined one.
     * A value of `null` indicates that the actual value does not value to be equal.
     */
    val equals: String? = null,

    /**
     * Matches only if the string is not exactly equal to the defined one.
     * A value of `null` indicates that there are no requirements to the inequality of the actual value.
     */
    val notEquals: String? = null,

    /**
     * Matches if the string conforms to the defined regex pattern.
     * A value of `null` indicates that the actual value does not have to match any regex pattern.
     */
    val matches: String? = null,

    /**
     * Matches if the string does not conform to the defined regex pattern.
     * A value of `null` indicates that there are no requirements to the inequality of the actual value.
     */
    @JsonProperty("not-matches")
    val notMatches: String? = null,

    /**
     * Matches if the string contains the defined one.
     * A value of `null` indicates that there are no requirements to the actual value containing a specified value.
     */
    val contains: String? = null,

    /**
     * Matches if the string does not contain the defined one.
     * A value of `null` indicates that there are no requirements to the actual value not containing a specified value.
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
         * equal to the given [expected].
         * @param expected Expected value that the actual value should be equal to.
         */
        @JvmStatic
        fun equalTo(expected: String): StringMatcher {
            return StringMatcher(equals = expected)
        }

        /**
         * Returns matcher that matches if the String value to be compared is equal
         * to the given [expected] while ignoring case sensitivity.
         * @param expected Expected value that the actual value should be equal to.
         */
        @JvmStatic
        fun equalToIgnoreCase(expected: String): StringMatcher {
            return StringMatcher(equals = expected, ignoreCase = true)
        }

        /**
         * Returns matcher that matches if the String value to be compared is not exactly
         * equal to the given [expected].
         * @param expected Expected value that the actual value should not be equal to.
         */
        @JvmStatic
        fun notEqualTo(expected: String): StringMatcher {
            return StringMatcher(equals = expected)
        }

        /**
         * Returns matcher that matches if the String value to be compared is not exactly
         * equal to the given [expected] while ignoring case sensitivity.
         * @param expected Expected value that the actual value should not be equal to.
         */
        @JvmStatic
        fun notEqualToIgnoreCase(expected: String): StringMatcher {
            return StringMatcher(notEquals = expected, ignoreCase = true)
        }

        /**
         * Returns matcher that matches if the String value to be compared conforms
         * to the given [regexPattern].
         * @param regexPattern Regular expression that the actual value should conform to.
         */
        @JvmStatic
        fun matching(regexPattern: String): StringMatcher {
            return StringMatcher(matches = regexPattern)
        }

        /**
         * Returns matcher that matches if the String value to be compared does not
         * conform to the given [regexPattern].
         * @param regexPattern Regular expression that the actual value should not conform to.
         */
        @JvmStatic
        fun notMatching(regexPattern: String): StringMatcher {
            return StringMatcher(notMatches = regexPattern)
        }

        /**
         * Returns matcher that matches if the String value to be compared contains
         * exactly the given [expected].
         * @param expected Expected value that the actual value should contain.
         */
        @JvmStatic
        fun containing(expected: String): StringMatcher {
            return StringMatcher(contains = expected)
        }

        /**
         * Returns matcher that matches if the String value to be compared contains
         * the given [expected] while ignoring case sensitivity.
         * @param expected Expected value that the actual value should contain.
         */
        @JvmStatic
        fun containingIgnoreCase(expected: String): StringMatcher {
            return StringMatcher(contains = expected, ignoreCase = true)
        }

        /**
         * Returns matcher that matches if the String value to be compared does not
         * contain exactly the given [expected].
         * @param expected Expected value that the actual value should not contain.
         */
        @JvmStatic
        fun notContaining(expected: String): StringMatcher {
            return StringMatcher(notContains = expected)
        }
    }

    /**
     * Chains this matcher with the given other [matcher] using a logical `AND`.
     * Matches only if both this and [matcher] match a given value.
     * @param matcher Other matcher to chain with this instance.
     */
    fun and(matcher: StringMatcher): LogicalAnd {
        return LogicalAnd(this, matcher)
    }

    /**
     * Chains this matcher with the given other [matcher] using a logical `OR`.
     * Matches if this and/or [matcher] match a given value.
     * @param matcher Other matcher to chain with this instance.
     */
    fun or(matcher: StringMatcher): LogicalOr {
        return LogicalOr(this, matcher)
    }

    /**
     * Returns whether the given string conforms to all of the requirements of this matcher.
     * @param value Actual value which should be compared to this matcher.
     * @return Whether the actual value conforms to all requirements of this matcher.
     */
    @JvmSynthetic
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

    /**
     * Returns whether the given string conforms to the equality requirements of this matcher.
     * @param value Actual value which should be compared to this matcher.
     * @return Whether the actual value conforms to the equality requirements of this matcher.
     */
    @JvmSynthetic
    internal fun matchesEqual(value: String): Boolean {
        if (this.equals != null && !value.equals(this.equals, this.ignoreCase)) {
            return false
        }

        if (this.notEquals != null && value.equals(this.notEquals, this.ignoreCase)) {
            return false
        }

        return true
    }

    /**
     * Returns whether the given string conforms to the regex pattern requirements of this matcher.
     * @param value Actual value which should be compared to this matcher.
     * @return Whether the actual value conforms to the regex pattern requirements of this matcher.
     */
    @JvmSynthetic
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

    /**
     * Returns whether the given string conforms to the containing requirements of this matcher.
     * @param value Actual value which should be compared to this matcher.
     * @return Whether the actual value conforms to the containing requirements of this matcher.
     */
    @JvmSynthetic
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
