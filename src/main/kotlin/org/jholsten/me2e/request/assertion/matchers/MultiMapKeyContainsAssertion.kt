package org.jholsten.me2e.request.assertion.matchers

/**
 * Assertion for checking if a map contains an expected key.
 */
class MultiMapKeyContainsAssertion<K>(private val expectedKey: K) : Assertable<Map<K, List<*>>?>(
    assertion = { actual -> actual?.containsKey(expectedKey) ?: false },
    message = { property, actual -> "Expected $property\n\t$actual\nto contain key\n\t$expectedKey" },
) {
    /**
     * Returns assertion for checking if the map contains the expected key with an expected value.
     */
    fun <V : Collection<E>, E> withValue(expectedValue: E): Assertable<Map<K, V>?> {
        return Assertable(
            assertion = { actual -> actual?.get(expectedKey)?.contains(expectedValue) ?: false },
            message = { property, actual -> "Expected $property\n\t$actual\nto contain\n\tkey $expectedKey with value $expectedValue" }
        )
    }

    fun <V : Collection<E>, E> withValues(expectedValues: V): Assertable<Map<K, V>?> {
        return Assertable(
            assertion = { actual -> actual?.get(expectedKey) == expectedValues },
            message = { property, actual -> "Expected $property\n\t$actual\nto contain\n\tkey $expectedKey with values $expectedValues" }
        )
    }
}
