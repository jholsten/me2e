package org.jholsten.me2e.assertions.matchers

/**
 * Assertion for checking if a map with a list of values contains an expected key.
 * For assertions concerning the value of the entry with the [expectedKey], use [withValue] and [withValues].
 * @param K Datatype of the keys of the maps to compare.
 */
class MultiMapKeyContainsAssertion<K>(private val expectedKey: K) : Assertable<Map<K, List<*>>?>(
    assertion = { actual -> actual?.containsKey(expectedKey) ?: false },
    message = "to contain key\n\t$expectedKey",
) {
    /**
     * Returns assertion for checking if the map contains the expected key with an expected value.
     * @param expectedValue Expected value that the entry for the [expectedKey] should have. TODO: Assertable
     * @param V Datatype of the values of the maps to compare.
     * @param E Datatype of the values of the list of values to compare.
     */
    fun <V : Collection<E>, E> withValue(expectedValue: E): Assertable<Map<K, V>?> {
        return Assertable(
            assertion = { actual -> actual?.get(expectedKey)?.contains(expectedValue) ?: false },
            message = "to contain key $expectedKey with value\n\t$expectedValue",
        )
    }

    /**
     * Returns assertion for checking if the map contains the expected key with all the expected values.
     * @param expectedValues Expected values that the entry for the [expectedKey] should have.
     * @param V Datatype of the values of the maps to compare.
     * @param E Datatype of the values of the list of values to compare.
     */
    fun <V : Collection<E>, E> withValues(expectedValues: V): Assertable<Map<K, V>?> {
        return Assertable(
            assertion = { actual -> actual?.get(expectedKey) == expectedValues },
            message = "to contain key $expectedKey with values\n\t$expectedValues",
        )
    }
}
