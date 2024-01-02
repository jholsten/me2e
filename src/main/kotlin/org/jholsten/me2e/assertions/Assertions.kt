package org.jholsten.me2e.assertions

import org.jholsten.me2e.assertions.matchers.*
import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.assertion.AssertableResponse

/**
 * Collection of assertions.
 */
class Assertions {
    companion object {
        /**
         * Returns [AssertableResponse] to assert that the properties of the given [response] are as expected.
         */
        @JvmStatic
        fun assertThat(response: HttpResponse): AssertableResponse {
            return AssertableResponse(response)
        }

        /**
         * Returns assertion for checking whether the expected value is equal to an actual value.
         */
        @JvmStatic
        fun <T> isEqualTo(expected: T): Assertable<T?> {
            return EqualityAssertion(expected)
        }

        /**
         * Returns assertion for checking whether an actual string value contains the given expected value.
         */
        @JvmStatic
        fun contains(expected: String): Assertable<String?> {
            return StringContainsAssertion(expected)
        }

        /**
         * Returns assertion for checking whether a map contains the given key.
         */
        @JvmStatic
        fun <K> containsKey(expectedKey: K): MultiMapKeyContainsAssertion<K> {
            return MultiMapKeyContainsAssertion(expectedKey)
        }

        /**
         * Returns assertion for checking whether an actual string value matches the given regex pattern.
         */
        @JvmStatic
        fun matchesPattern(expectedPattern: String): Assertable<String?> {
            return PatternMatchAssertion(expectedPattern)
        }

        /**
         * Returns assertion for checking whether an actual value is null.
         */
        @JvmStatic
        fun <T> isNull(): Assertable<T?> {
            return NullAssertion()
        }

        /**
         * Returns assertion for checking whether an actual value is not null.
         */
        @JvmStatic
        fun <T> isNotNull(): Assertable<T?> {
            return NotNullAssertion()
        }
    }
}
