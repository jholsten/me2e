package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
import org.jholsten.me2e.utils.toJson
import java.time.Duration
import java.time.Instant

/**
 * Summary of a test or test container for which the execution was finished.
 * Includes succeeded, failed and aborted tests and test containers.
 */
class FinishedTestResult(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    testId: String,

    /**
     * Summaries of the children of this test or test container.
     * For instance, if this summary describes a Test Class, the children include all tests of this class.
     * For a leaf, this list is empty.
     */
    children: List<TestResult>,

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
     * Number of tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `1`.
     * However, if this result relates to a test container, the value
     * reflects the number of tests contained in the container.
     */
    numberOfTests: Int,

    /**
     * Number of failed tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `0` or `1`, depending on the status.
     * However, if this result relates to a test container, the value
     * reflects the number of failed tests contained in the container.
     */
    numberOfFailures: Int,

    /**
     * Number of skipped tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `0`.
     * However, if this result relates to a test container, the value
     * reflects the number of skipped tests contained in the container.
     */
    numberOfSkipped: Int,

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
) : TestResult(
    testId = testId,
    children = children,
    status = status,
    numberOfTests = numberOfTests,
    numberOfFailures = numberOfFailures,
    numberOfSkipped = numberOfSkipped,
    displayName = displayName,
    tags = tags,
) {
    /**
     * Number of seconds that the test execution took.
     * As the [startTime] and [endTime] are recorded by the [org.jholsten.me2e.report.result.ReportDataAggregator],
     * there may be small deviations from the values in the JUnit test report.
     */
    val duration: Long = Duration.between(startTime, endTime).seconds

    override fun toString(): String = toJson(this)
}
