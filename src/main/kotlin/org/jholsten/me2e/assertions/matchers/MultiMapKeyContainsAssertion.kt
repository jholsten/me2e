package org.jholsten.me2e.assertions.matchers

import org.jholsten.me2e.assertions.AssertionFailure

/**
 * Assertion for checking if a map with a list of values contains an expected key.
 * For assertions concerning the value of the entry with the [expectedKey], use [withValue] and [withValues].
 * @param K Datatype of the keys of the maps to compare.
 */
class MultiMapKeyContainsAssertion<K> internal constructor(private val expectedKey: K) : Assertable<Map<K, List<*>>?>(
    assertion = { actual -> actual?.containsKey(expectedKey) ?: false },
    message = "to contain key\n\t$expectedKey",
) {
    /**
     * Returns assertion for checking if the map contains the expected key with an entry which contains a
     * value which satisfies the given expectation.
     * @param expectedValue Expectation for the value of the entry for the [expectedKey].
     * @param V Datatype of the values of the maps to compare.
     * @param E Datatype of the values of the list of values to compare.
     */
    fun <V : Collection<E>, E> withValue(expectedValue: Assertable<E>): Assertable<Map<K, V>?> {
        return object : Assertable<Map<K, V>?>(
            assertion = { actual -> evaluateValue(actual?.get(expectedKey), expectedValue) },
            message = "to contain key $expectedKey with value\n\t$expectedValue",
        ) {
            override fun evaluate(property: String, actual: Map<K, V>?) {
                val entry = actual?.get(expectedKey)
                    ?: throw AssertionFailure("Expected $property\n\t$actual\nto contain key\n\t$expectedKey")
                if (!evaluateValue(entry, expectedValue)) {
                    val message = "Expected $property\n\t$actual\nto contain key $expectedKey with value\n" +
                        "\t$entry\nto contain at least one value ${expectedValue.message}"
                    throw AssertionFailure(message)
                }
            }

            override fun toString(): String = "contains key $expectedKey with value $expectedValue"
        }
    }

    /**
     * Returns assertion for checking if the map contains the expected key with all the expected values.
     * @param expectedValues Expected values that the entry for the [expectedKey] should have.
     * @param V Datatype of the values of the maps to compare.
     * @param E Datatype of the values of the list of values to compare.
     */
    fun <V : Collection<E>, E> withValues(expectedValues: V): Assertable<Map<K, V>?> {
        return object : Assertable<Map<K, V>?>(
            assertion = { actual -> actual?.get(expectedKey) == expectedValues },
            message = "to contain key $expectedKey with values\n\t$expectedValues",
        ) {
            override fun evaluate(property: String, actual: Map<K, V>?) {
                val entry = actual?.get(expectedKey)
                    ?: throw AssertionFailure("Expected $property\n\t$actual\nto contain key\n\t$expectedKey")
                if (entry != expectedValues) {
                    val message = "Expected $property\n\t$actual\nto contain key $expectedKey " +
                        "with values\n\t$entry\nto be equal to\n\t$expectedValues"
                    throw AssertionFailure(message)
                }
            }

            override fun toString(): String = "contains key $expectedKey with values $expectedValues"
        }
    }

    /**
     * Returns whether any value in the list of values of the given entry satisfies the given assertion.
     * @param entry Entry of the map for which the values are to be evaluated.
     * @param expected Expectation for the value of the entry for the [expectedKey].
     */
    private fun <V : Collection<E>, E> evaluateValue(entry: V?, expected: Assertable<E>): Boolean {
        return entry?.any { expected.assertion(it) } == true
    }

    override fun toString(): String = "contains key $expectedKey"
}
