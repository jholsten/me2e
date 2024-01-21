package org.jholsten.me2e.report.result.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TestResultUtilsTest {

    @Test
    fun `Calculating success rate should return the correct result`() {
        assertEquals(91, calculateSuccessRate(105, 9, 5))
    }

    @Test
    fun `Calculating success rate in case all tests are skipped should return null`() {
        assertNull(calculateSuccessRate(100, 0, 100))
    }
}
