package org.jholsten.util

import org.assertj.core.api.Assertions
import java.lang.AssertionError

class RecursiveComparison {

    companion object {
        @JvmStatic
        fun <T> assertEquals(expected: T, actual: T) {
            Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }

        @JvmStatic
        fun <T> isEqualTo(expected: T, actual: T): Boolean {
            return try {
                assertEquals(expected, actual)
                true
            } catch (e: AssertionError) {
                println(e)
                false
            }
        }
    }
}
