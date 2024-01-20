package org.jholsten.me2e.report.summary.model

import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
import org.jholsten.me2e.utils.toJson
import java.time.Instant

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
     * Timestamp of when this test or test container has started its execution.
     */
    val startTime: Instant,

    /**
     * Timestamp of when this test or test container has finished its execution.
     */
    val endTime: Instant,

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
     * Resource usage statistics that were collected for this test execution.
     */
    val stats: AggregatedStatsEntryList,

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
