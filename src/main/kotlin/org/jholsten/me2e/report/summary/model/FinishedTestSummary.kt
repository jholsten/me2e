package org.jholsten.me2e.report.summary.model

import org.jholsten.me2e.report.logs.AggregatedLogEntryList
import org.jholsten.me2e.utils.toJson

/**
 * Summary of a test or test container for which the execution was finished.
 * Includes succeeded, failed and aborted tests and test containers.
 */
class FinishedTestSummary(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    testId: String,

    /**
     * ID of the parent of this test or test container.
     * An identifier without a parent is called a `root`.
     * @see org.junit.platform.launcher.TestIdentifier.getParentId
     */
    parentId: String?,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    status: Status,

    /**
     * Human-readable name of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getDisplayName
     */
    displayName: String,

    /**
     * Tags associated with the represented test or test container.
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
    parentId = parentId,
    status = status,
    displayName = displayName,
    tags = tags,
) {
    override fun toString(): String = toJson(this)
}
