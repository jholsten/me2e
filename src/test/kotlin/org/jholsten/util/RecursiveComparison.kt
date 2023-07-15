package org.jholsten.util

import org.assertj.core.api.Assertions

class RecursiveComparison {
    
    companion object {
        fun <T> assertEquals(expected: T, actual: T) {
            Assertions.assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }
    }
}
