@file:JvmName("Assertions")

package org.jholsten.me2e.assertions

import org.jholsten.me2e.assertions.matchers.*
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.verification.MockServerVerification
import org.jholsten.me2e.request.assertions.AssertableResponse
import org.jholsten.me2e.request.model.HttpResponse

/**
 * Returns [AssertableResponse] to assert that the properties of the given [response] are as expected.
 * @param response Response to whose properties the assertions refer.
 * @see AssertableResponse
 */
fun assertThat(response: HttpResponse): AssertableResponse {
    return AssertableResponse(response)
}

/**
 * Returns [MockServerVerification] to assert that the [mockServer] received an expected request.
 * @param mockServer Mock Server which should have received the expected request.
 * @see MockServerVerification
 */
fun assertThat(mockServer: MockServer): MockServerVerification {
    return MockServerVerification(mockServer)
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
 * Returns assertion for checking whether an actual value is equal to the contents from the
 * file with the given [filename].
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).body(equalToContentsFromFile("expected_string_content.txt").asString())
 * assertThat(response).jsonBody(equalToContentsFromFile("expected_json_content.json").asJson())
 * assertThat(response).binaryBody(equalToContentsFromFile("expected_binary_content").asBinary())
 * ```
 * @param filename Name of the file which contains the expected value. Needs to be located in
 * `resources` folder.
 * @throws java.io.FileNotFoundException if file with the given name does not exist.
 */
fun equalToContentsFromFile(filename: String): FileContentsEqualityAssertion {
    return FileContentsEqualityAssertion(filename)
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
 * assertThat(response).protocol(containsString("HTTP"))
 * ```
 * @param expected Expected value which should be contained in the actual value.
 */
fun containsString(expected: String): Assertable<String?> {
    return StringContainsAssertion(expected)
}

/**
 * Returns assertion for checking whether a map contains the given key,
 * i.e. an assertion which does not throw if `actual.containsKey(expectedKey)`.
 * For assertions concerning the value of the entry with the [expectedKey], use
 * [MultiMapKeyContainsAssertion.withValue] and [MultiMapKeyContainsAssertion.withValues].
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).headers(containsKey("Content-Type").withValue(equalTo("application/json")))
 * ```
 * @param expectedKey Expected key which should be contained in the actual map.
 * @param K Datatype of the keys of the maps to compare.
 */
fun <K> containsKey(expectedKey: K): MultiMapKeyContainsAssertion<K> {
    return MultiMapKeyContainsAssertion(expectedKey)
}

/**
 * Returns assertion for checking whether a JSON body contains a node with the given path.
 * Use `.` as path separators to specify a path in the JSON tree. Use `[{index}]` to specify the element at index `index`
 * in an array node. See [JsonBodyAssertion] for detailed information on the format of the [JsonBodyAssertion.expectedPath].
 * For assertions concerning the value of the node with the [expectedPath], use [JsonBodyAssertion.withValue].
 *
 * Example Usage:
 * ```kotlin
 * assertThat(response).jsonBody(containsNode("journal.title").withValue(equalTo("IEEE Software")))
 * ```
 * @param expectedPath Expected path to the JSON node which should be contained in the actual JSON body.
 */
fun containsNode(expectedPath: String): JsonBodyAssertion {
    return JsonBodyAssertion(expectedPath)
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
