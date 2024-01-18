package org.jholsten.me2e.report.summary.model

import org.jholsten.me2e.report.logs.AggregatedLogEntryList

/**
 * Summary of a test for which the execution was finished.
 * Includes succeeded, failed and aborted tests.
 */
class FinishedTestSummary(
    /**
     * Unique identifier of the test.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    testId: String,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    status: Status,

    /**
     * Human-readable name of the test.
     * @see org.junit.platform.launcher.TestIdentifier.getDisplayName
     */
    displayName: String,

    /**
     * Tags associated with the represented test.
     * @see org.junit.platform.launcher.TestIdentifier.getTags
     */
    tags: Set<String>,

    /**
     * Additional report entries that were published during the test execution.
     */
    val reportEntries: List<ReportEntry>,

    /**
     * Logs that were collected for this test execution.
     * Includes test runner logs as well as Docker container logs.
     */
    val logs: AggregatedLogEntryList,

    /**
     * Throwable that caused this result.
     * @see org.junit.platform.engine.TestExecutionResult.getThrowable
     */
    val throwable: Throwable?,
) : TestSummary(
    testId = testId,
    status = status,
    displayName = displayName,
    tags = tags,
)
