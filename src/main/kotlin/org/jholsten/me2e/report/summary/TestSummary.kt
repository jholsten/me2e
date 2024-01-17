package org.jholsten.me2e.report.summary

import org.jholsten.me2e.report.logs.AggregatedLogEntryList
import java.time.LocalDateTime

/**
 * Summary of the execution of a single test.
 * TODO: Maybe extend this for containers also.
 */
class TestSummary(
    /**
     * Unique identifier of the test.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    val testId: String,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    val status: Status,

    /**
     * Human-readable name of the test.
     * @see org.junit.platform.launcher.TestIdentifier.getDisplayName
     */
    val displayName: String,

    /**
     * Tags associated with the represented test.
     * @see org.junit.platform.launcher.TestIdentifier.getTags
     */
    val tags: Set<String>,

    /**
     * Additional report entries that were published during the test execution.
     * TODO: Add model
     */
    val reportEntries: List<Map<String, Pair<LocalDateTime, String>>>,

    /**
     * Logs that were collected for this test execution.
     */
    val logs: AggregatedLogEntryList,
) {
    /**
     * Status of executing a single test.
     * @see org.junit.platform.engine.TestExecutionResult.Status
     */
    enum class Status {
        /**
         * Indicates that the execution of the test was successful.
         */
        SUCCESSFUL,

        /**
         * Indicates that the execution of the test was aborted (started but not finished).
         */
        ABORTED,

        /**
         * Indicates that the execution of the test failed.
         */
        FAILED,

        /**
         * Indicates that the execution of the test was skipped.
         * TODO: Maybe provide subclass SkippedTest to store reason.
         */
        SKIPPED,
    }
}
