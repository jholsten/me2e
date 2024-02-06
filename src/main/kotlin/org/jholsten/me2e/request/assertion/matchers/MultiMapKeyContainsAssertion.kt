package org.jholsten.me2e.request.assertion.matchers

/**
 * Assertion for checking if a map contains an expected key.
 * For assertions concerning the value of the entry with the [expectedKey], use [withValue] and [withValues].
 */
class MultiMapKeyContainsAssertion<K>(private val expectedKey: K) : Assertable<Map<K, List<*>>?>(
    assertion = { actual -> actual?.containsKey(expectedKey) ?: false },
    message = { property, actual -> "Expected $property\n\t$actual\nto contain key\n\t$expectedKey" },
) {
    /**
     * Returns assertion for checking if the map contains the expected key with an expected value.
     * @param expectedValue Expected value that the entry for the [expectedKey] should have.
     */
    fun <V : Collection<E>, E> withValue(expectedValue: E): Assertable<Map<K, V>?> {
        return Assertable(
            assertion = { actual -> actual?.get(expectedKey)?.contains(expectedValue) ?: false },
            message = { property, actual -> "Expected $property\n\t$actual\nto contain\n\tkey $expectedKey with value $expectedValue" }
        )
    }

    /**
     * Returns assertion for checking if the map contains the expected key with all the expected values.
     * @param expectedValues Expected values that the entry for the [expectedKey] should have.
     */
    fun <V : Collection<E>, E> withValues(expectedValues: V): Assertable<Map<K, V>?> {
        return Assertable(
            assertion = { actual -> actual?.get(expectedKey) == expectedValues },
            message = { property, actual -> "Expected $property\n\t$actual\nto contain\n\tkey $expectedKey with values $expectedValues" }
        )
    }
}
