package org.jholsten.me2e.report.result.utils

import java.math.BigDecimal
import java.time.Instant
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

    @Test
    fun `Calculating duration in seconds should return the correct result`() {
        val startTime = Instant.now()
        val endTime = startTime.plusSeconds(5).plusMillis(753)
        assertEquals(BigDecimal("5.753"), calculateDurationInSeconds(startTime, endTime))
    }
}
