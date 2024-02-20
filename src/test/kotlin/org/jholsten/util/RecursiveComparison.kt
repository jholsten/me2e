package org.jholsten.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import java.lang.AssertionError

class RecursiveComparison {

    companion object {
        @JvmStatic
        fun <T> assertEquals(
            expected: T,
            actual: T,
            ignoreCollectionOrder: Boolean = false,
            fieldsToIgnore: List<String> = listOf(),
            comparatorForFields: Pair<Comparator<*>, List<String>>? = null,
        ) {
            val config = RecursiveComparisonConfiguration.builder()
                .withIgnoreCollectionOrder(ignoreCollectionOrder)
                .withIgnoredFields(*fieldsToIgnore.toTypedArray())

            if (comparatorForFields != null) {
                config.withComparatorForFields(comparatorForFields.first, *comparatorForFields.second.toTypedArray())
            }

            Assertions.assertThat(actual).usingRecursiveComparison(config.build()).isEqualTo(expected)
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
