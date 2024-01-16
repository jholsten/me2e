package org.jholsten.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import java.lang.AssertionError

class RecursiveComparison {

    companion object {
        @JvmStatic
        fun <T> assertEquals(expected: T, actual: T, ignoreCollectionOrder: Boolean = false) {
            val config = RecursiveComparisonConfiguration.builder()
                .withIgnoreCollectionOrder(ignoreCollectionOrder)
                .build()

            Assertions.assertThat(actual).usingRecursiveComparison(config).isEqualTo(expected)
        }

        @JvmStatic
        fun <T> isEqualTo(expected: T, actual: T, ignoreCollectionOrder: Boolean = false): Boolean {
            return try {
                assertEquals(expected, actual, ignoreCollectionOrder)
                true
            } catch (e: AssertionError) {
                println(e)
                false
            }
        }
    }
}
