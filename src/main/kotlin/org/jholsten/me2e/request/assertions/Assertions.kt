@file:JvmName("Assertions")

package org.jholsten.me2e.request.assertions

import org.jholsten.me2e.request.model.HttpResponse
import org.jholsten.me2e.request.assertions.matchers.*

/**
 * Returns [AssertableResponse] to assert that the properties of the given [response] are as expected.
 * @param response Response to whose properties the assertions refer.
 */
fun assertThat(response: HttpResponse): AssertableResponse {
    return AssertableResponse(response)
}

/**
 * Returns assertion for checking whether the expected value is equal to an actual value,
 * i.e. an assertion which does not throw if `actual == expected`.
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).statusCode(equalTo(200))
 * ```
 * @param expected Expected value which should be equal to the actual value.
 * @param T Datatype of the values to compare.
 */
fun <T> equalTo(expected: T): Assertable<T?> {
    return EqualityAssertion(expected)
}

/**
 * Returns assertion for checking whether the expected value is not equal to an actual value,
 * i.e. an assertion which does not throw if `actual != expected`.
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).statusCode(notEqualTo(500))
 * ```
 * @param expected Expected value which should not be equal to the actual value.
 * @param T Datatype of the values to compare.
 */
fun <T> notEqualTo(expected: T): Assertable<T?> {
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
fun matchesPattern(expectedPattern: String): Assertable<String?> {
    return PatternMatchAssertion(expectedPattern)
}

/**
 * Returns assertion for checking whether an actual numeric value is greater than the given value,
 * i.e. an assertion which does not throw if `actual > expected`.
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).statusCode(greaterThan(100))
 * ```
 * @param expected Numeric value which should be less than the actual value.
 * @param T Numeric datatype of the values to compare.
 */
fun <T> greaterThan(expected: T): Assertable<T?> where T : Number?, T : Comparable<T> {
    return GreaterThanAssertion(expected)
}

/**
 * Returns assertion for checking whether an actual numeric value is less than the given value,
 * i.e. an assertion which does not throw if `actual < expected`.
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).statusCode(lessThan(500))
 * ```
 * @param expected Numeric value which should be greater than the actual value.
 * @param T Numeric datatype of the values to compare.
 */
fun <T> lessThan(expected: T): Assertable<T?> where T : Number?, T : Comparable<T> {
    return LessThanAssertion(expected)
}

/**
 * Returns assertion for checking whether an actual numeric value is within the given range of values,
 * i.e. an assertion which does not throw if `lowerBound <= actual <= upperBound`.
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).statusCode(between(200, 299))
 * ```
 * @param lowerBound Numeric value which should be less than or equal to the actual value.
 * @param upperBound Numeric value which should be greater than or equal to the actual value.
 * @param T Numeric datatype of the values to compare.
 */
fun <T> between(lowerBound: T, upperBound: T): Assertable<T?> where T : Number?, T : Comparable<T> {
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
fun <T> isNotNull(): Assertable<T?> {
    return NotNullAssertion()
}
