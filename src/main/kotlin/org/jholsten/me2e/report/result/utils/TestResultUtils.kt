@file:JvmSynthetic

package org.jholsten.me2e.report.result.utils

import java.math.BigDecimal
import java.math.MathContext
import java.time.Duration
import java.time.Instant

/**
 * Calculates the success rate of a test result using the given metrics.
 * The success rate is defined as the relative share of successful tests
 * in the total number of tests, while ignoring skipped tests.
 * Returns `null` in case the result contains only skipped tests (i.e.
 * [numberOfSkipped] is equal to [numberOfTests]).
 * @param numberOfTests Number of tests that were executed.
 * @param numberOfFailures Number of failed tests.
 * @param numberOfSkipped Number of skipped tests.
 * @return Success rate in percent, rounded to the nearest integer.
 */
internal fun calculateSuccessRate(numberOfTests: Int, numberOfFailures: Int, numberOfSkipped: Int): Int? {
    val total = BigDecimal(numberOfTests - numberOfSkipped)
    return if (total == BigDecimal.ZERO) {
        null
    } else {
        val success = total - BigDecimal(numberOfFailures)
        (success.divide(total, MathContext(2)) * BigDecimal(100)).round(MathContext(0)).toInt()
    }
}

/**
 * Calculates the number of seconds between [startTime] and [endTime].
 */
internal fun calculateDurationInSeconds(startTime: Instant, endTime: Instant): BigDecimal {
    val duration = Duration.between(startTime, endTime)
    val seconds = duration.seconds
    val milliseconds = duration.toMillisPart()
    return (BigDecimal(seconds) + BigDecimal(milliseconds).divide(BigDecimal(1000), MathContext(3))).setScale(3)
}
