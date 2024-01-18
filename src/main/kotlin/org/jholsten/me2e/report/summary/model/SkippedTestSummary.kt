package org.jholsten.me2e.report.summary.model

import org.jholsten.me2e.report.logs.AggregatedLogEntryList

/**
 * Summary of a test for which the execution was skipped.
 */
class SkippedTestSummary(
    /**
     * Unique identifier of the test.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    testId: String,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    status: TestSummary.Status,

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
     * Message describing why the execution has been skipped.
     */
    val reason: String?,
) : TestSummary(
    testId = testId,
    status = status,
    displayName = displayName,
    tags = tags,
)
