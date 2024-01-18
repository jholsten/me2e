package org.jholsten.me2e.report.summary.model

import org.jholsten.me2e.report.logs.AggregatedLogEntryList
import org.jholsten.me2e.report.summary.mapper.TestSummaryStatusMapper
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestIdentifier

/**
 * Summary of the execution of a single test.
 * TODO: Maybe extend this for containers also.
 */
abstract class TestSummary(
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
         */
        SKIPPED,
    }

    companion object {
        /**
         * Creates an instance of [FinishedTestSummary] representing a test for which the execution was finished.
         * @param testIdentifier Identifier of the finished test.
         * @param testExecutionResult Result of the execution for the supplied [testIdentifier].
         * @param reportEntries Report entries that were published during the test execution.
         * @param logs Logs that were collected during the test execution.
         */
        @JvmSynthetic
        internal fun finished(
            testIdentifier: TestIdentifier,
            testExecutionResult: TestExecutionResult,
            reportEntries: List<ReportEntry>,
            logs: AggregatedLogEntryList,
        ): FinishedTestSummary {
            return FinishedTestSummary(
                testId = testIdentifier.uniqueId,
                status = TestSummaryStatusMapper.INSTANCE.toInternalDto(testExecutionResult.status),
                displayName = testIdentifier.displayName,
                tags = testIdentifier.tags.map { it.name }.toSet(),
                reportEntries = reportEntries.toList(),
                logs = logs,
                throwable = testExecutionResult.throwable.orElse(null),
            )
        }

        /**
         * Creates an instance of [SkippedTestSummary] representing a test for which the execution was skipped.
         * @param testIdentifier Identifier of the skipped test.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        internal fun skipped(
            testIdentifier: TestIdentifier,
            reason: String?,
        ): SkippedTestSummary {
            return SkippedTestSummary(
                testId = testIdentifier.uniqueId,
                status = Status.SKIPPED,
                displayName = testIdentifier.displayName,
                tags = testIdentifier.tags.map { it.name }.toSet(),
                reason = reason,
            )
        }
    }
}
