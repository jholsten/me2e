package org.jholsten.me2e.request.assertion

import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.assertion.matchers.*

/**
 * Factory methods for the assertions for the properties of an [HttpResponse].
 *
 * Example Usage:
 * ```kotlin
 * import org.jholsten.me2e.Me2eTest
 * import org.jholsten.me2e.container.injection.InjectService
 * import org.jholsten.me2e.container.microservice.MicroserviceContainer
 * import org.jholsten.me2e.request.assertion.Assertions.Companion.assertThat
 * import org.jholsten.me2e.request.assertion.Assertions.Companion.isEqualTo
 * import org.jholsten.me2e.request.model.*
 * import kotlin.test.*
 *
 * class E2ETest : Me2eTest() {
 *     @InjectService
 *     private lateinit var api: MicroserviceContainer
 *
 *     @Test
 *     fun `Invoking endpoint should return expected request`() {
 *         val url = RelativeUrl.Builder().withPath("/books").withQueryParameter("id", "1234").build()
 *         val response = api.get(url)
 *
 *         assertThat(response)
 *             .statusCode(isEqualTo(200))
 *             .message(isEqualTo("OK"))
 *             .jsonBody("journal.title", isEqualTo("IEEE Software"))
 *     }
 * }
 * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).statusCode(isEqualTo(200))
         * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).statusCode(isNotEqualTo(500))
         * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).protocol(contains("HTTP"))
         * ```
         * @param expected Expected value which should be contained in the actual value.
         */
        @JvmStatic
        fun contains(expected: String): Assertable<String?> {
            return StringContainsAssertion(expected)
        }

        /**
         * Returns assertion for checking whether a map contains the given key,
         * i.e. an assertion which does not throw if `actual.containsKey(expectedKey)`.
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).headers(containsKey("Content-Type").withValue("application/json"))
         * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).protocol(matchesPattern("HTTP/.{3}"))
         * ```
         * @param expectedPattern Expected pattern to which the actual value should match.
         */
        @JvmStatic
        fun matchesPattern(expectedPattern: String): Assertable<String?> {
            return PatternMatchAssertion(expectedPattern)
        }

        /**
         * Returns assertion for checking whether an actual numeric value is greater than the given value,
         * i.e. an assertion which does not throw if `actual > expected`.
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).statusCode(isGreaterThan(100))
         * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).statusCode(isLessThan(500))
         * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).statusCode(isBetween(200, 299))
         * ```
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
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).jsonBody("value", isNull())
         * ```
         * @param T Datatype of the value to check.
         */
        @JvmStatic
        fun <T> isNull(): Assertable<T?> {
            return NullAssertion()
        }

        /**
         * Returns assertion for checking whether an actual value is not null.
         *
         * Example Usage:
         * ```kotlin
         * assertThat(response).body(isNotNull())
         * ```
         * @param T Datatype of the value to check.
         */
        @JvmStatic
        fun <T> isNotNull(): Assertable<T?> {
            return NotNullAssertion()
        }
    }
}
