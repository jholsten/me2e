package org.jholsten.me2e.report.result.model

import java.time.Instant

/**
 * Report entry published by a test or test container which contains
 * additional information to be published in the test report.
 * @see org.junit.platform.engine.reporting.ReportEntry
 */
data class ReportEntry(
    /**
     * Timestamp of when this report entry was created.
     */
    val timestamp: Instant,

    /**
     * Key value pairs that were published during the execution.
     */
    val keyValuePairs: Map<String, String>,
)
