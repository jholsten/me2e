package org.jholsten.me2e.mock.stubbing.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.intellij.lang.annotations.Language
import org.jholsten.me2e.utils.toJson

/**
 * Pattern to match string values.
 * This matcher is used to match the properties of the request stubs to the requests that the Mock Server receives.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class StringMatcher internal constructor(
    /**
     * Matches only if the string is exactly equal to the defined one.
     * A value of `null` indicates that the actual value does not value to be equal.
     */
    val equals: String? = null,

    /**
     * Matches only if the string is not exactly equal to the defined one.
     * A value of `null` indicates that there are no requirements to the inequality of the actual value.
     */
    @JsonProperty("not-equals")
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
    /**
     * Returns whether the given string conforms to all of the requirements of this matcher.
     * @param value Actual value which should be compared to this matcher.
     * @return Whether the actual value conforms to all requirements of this matcher.
     */
    @JvmSynthetic
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
    internal fun matchesPattern(@Language("RegExp") value: String): Boolean {
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
