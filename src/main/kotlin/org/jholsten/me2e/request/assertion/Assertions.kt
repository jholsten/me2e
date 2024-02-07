package org.jholsten.me2e.request.assertion

import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.assertion.matchers.*

/**
 * Factory methods for the assertions for the properties of an [HttpResponse].
 */
class Assertions {
    companion object {
        /**
         * Returns [AssertableResponse] to assert that the properties of the given [response] are as expected.
         * @param response Response to whose properties the assertions refer.
         */
        @JvmStatic
        fun assertThat(response: HttpResponse): AssertableResponse {
            return AssertableResponse(response)
        }

        /**
         * Returns assertion for checking whether the expected value is equal to an actual value,
         * i.e. an assertion which does not throw if `actual == expected`.
         * @param expected Expected value which should be equal to the actual value.
         * @param T Datatype of the values to compare.
         */
        @JvmStatic
        fun <T> isEqualTo(expected: T): Assertable<T?> {
            return EqualityAssertion(expected)
        }

        /**
         * Returns assertion for checking whether the expected value is not equal to an actual value,
         * i.e. an assertion which does not throw if `actual != expected`.
         * @param expected Expected value which should not be equal to the actual value.
         * @param T Datatype of the values to compare.
         */
        @JvmStatic
        fun <T> isNotEqualTo(expected: T): Assertable<T?> {
            return InEqualityAssertion(expected)
        }

        /**
         * Returns assertion for checking whether an actual string value contains the given expected value,
         * i.e. an assertion which does not throw if `actual.contains(expected)`.
         * @param expected Expected value which should be contained in the actual value.
         */
        @JvmStatic
        fun contains(expected: String): Assertable<String?> {
            return StringContainsAssertion(expected)
        }

        /**
         * Returns assertion for checking whether a map contains the given key,
         * i.e. an assertion which does not throw if `actual.containsKey(expectedKey)`.
         * @param expectedKey Expected key which should be contained in the actual map.
         * @param K Datatype of the keys of the maps to compare.
         */
        @JvmStatic
        fun <K> containsKey(expectedKey: K): MultiMapKeyContainsAssertion<K> {
            return MultiMapKeyContainsAssertion(expectedKey)
        }

        /**
         * Returns assertion for checking whether an actual string value matches the given regex pattern,
         * i.e. an assertion which does not throw if `expected.matches(actual)`.
         * @param expectedPattern Expected pattern to which the actual value should match.
         */
        @JvmStatic
        fun matchesPattern(expectedPattern: String): Assertable<String?> {
            return PatternMatchAssertion(expectedPattern)
        }

        /**
         * Returns assertion for checking whether an actual numeric value is greater than the given value,
         * i.e. an assertion which does not throw if `actual > expected`.
         * @param expected Numeric value which should be less than the actual value.
         * @param T Numeric datatype of the values to compare.
         */
        @JvmStatic
        fun <T> isGreaterThan(expected: T): Assertable<T?> where T : Number?, T : Comparable<T> {
            return GreaterThanAssertion(expected)
        }

        /**
         * Returns assertion for checking whether an actual numeric value is less than the given value,
         * i.e. an assertion which does not throw if `actual < expected`.
         * @param expected Numeric value which should be greater than the actual value.
         * @param T Numeric datatype of the values to compare.
         */
        @JvmStatic
        fun <T> isLessThan(expected: T): Assertable<T?> where T : Number?, T : Comparable<T> {
            return LessThanAssertion(expected)
        }

        /**
         * Returns assertion for checking whether an actual numeric value is within the given range of values,
         * i.e. an assertion which does not throw if `lowerBound <= actual <= upperBound`.
         * @param lowerBound Numeric value which should be less than or equal to the actual value.
         * @param upperBound Numeric value which should be greater than or equal to the actual value.
         * @param T Numeric datatype of the values to compare.
         */
        @JvmStatic
        fun <T> isBetween(lowerBound: T, upperBound: T): Assertable<T?> where T : Number?, T : Comparable<T> {
            return BetweenAssertion(lowerBound, upperBound)
        }

        /**
         * Returns assertion for checking whether an actual value is null.
         * @param T Datatype of the value to check.
         */
        @JvmStatic
        fun <T> isNull(): Assertable<T?> {
            return NullAssertion()
        }

        /**
         * Returns assertion for checking whether an actual value is not null.
         * @param T Datatype of the value to check.
         */
        @JvmStatic
        fun <T> isNotNull(): Assertable<T?> {
            return NotNullAssertion()
        }
    }
}
