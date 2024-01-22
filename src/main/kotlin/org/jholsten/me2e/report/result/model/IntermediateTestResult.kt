@file:JvmSynthetic

package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
import org.jholsten.me2e.report.result.mapper.TestSummaryStatusMapper
import org.jholsten.me2e.utils.toJson
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestIdentifier
import java.time.Instant

/**
 * Intermediate summary of the execution of a single test or test container.
 * Contains all data for a test and its result during the test execution.
 * As certain information can only be determined after all tests have been
 * executed, this model is converted into a [TestResult] after the execution
 * of all tests has finished.
 */
internal class IntermediateTestResult(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    val testId: String,

    /**
     * ID of the parent of this test or test container.
     * An identifier without a parent is called a `root`.
     * @see org.junit.platform.launcher.TestIdentifier.getParentId
     */
    var parentId: String?,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    val status: TestResult.Status,

    /**
     * Timestamp of when this test or test container has started its execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    val startTime: Instant? = null,

    /**
     * Timestamp of when this test or test container has finished its execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    val endTime: Instant? = null,

    /**
     * Human-readable name of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getDisplayName
     */
    val displayName: String,

    /**
     * Tags associated with the represented test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getTags
     */
    val tags: Set<String>,

    /**
     * Additional report entries that were published during the test execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    val reportEntries: List<ReportEntry>? = null,

    /**
     * Logs that were collected for this test execution.
     * Includes test runner logs as well as Docker container logs.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    val logs: AggregatedLogEntryList? = null,

    /**
     * Resource usage statistics that were collected for this test execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    val stats: AggregatedStatsEntryList? = null,

    /**
     * Throwable that caused this result.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     * @see org.junit.platform.engine.TestExecutionResult.getThrowable
     */
    val throwable: Throwable? = null,

    /**
     * Message describing why the execution has been skipped.
     * Only set for tests with status [TestResult.Status.SKIPPED].
     */
    val skippingReason: String? = null,
) {
    companion object {
        /**
         * Creates an instance of [IntermediateTestResult] representing a test or test container for which the execution was finished.
         * @param testIdentifier Identifier of the finished test or test container.
         * @param testExecutionResult Result of the execution for the supplied [testIdentifier].
         * @param startTime Timestamp of when this test or test container has started its execution.
         * @param reportEntries Report entries that were published during the test execution.
         * @param logs Logs that were collected during the test execution.
         * @param stats Resource usage statistics that were collected during the test execution.
         */
        @JvmSynthetic
        internal fun finished(
            testIdentifier: TestIdentifier,
            testExecutionResult: TestExecutionResult,
            startTime: Instant?,
            reportEntries: List<ReportEntry>,
            logs: AggregatedLogEntryList,
            stats: AggregatedStatsEntryList,
        ): IntermediateTestResult {
            return IntermediateTestResult(
                testId = testIdentifier.uniqueId,
                parentId = testIdentifier.parentId.orElse(null),
                status = TestSummaryStatusMapper.INSTANCE.toInternalDto(testExecutionResult.status),
                startTime = startTime ?: Instant.now(),
                endTime = Instant.now(),
                displayName = testIdentifier.displayName.substringBeforeLast("("),
                tags = testIdentifier.tags.map { it.name }.toSet(),
                reportEntries = reportEntries.toList(),
                logs = logs,
                stats = stats,
                throwable = testExecutionResult.throwable.orElse(null),
            )
        }

        /**
         * Creates an instance of [IntermediateTestResult] representing a test or test container for which the execution was skipped.
         * @param testIdentifier Identifier of the skipped test.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        internal fun skipped(
            testIdentifier: TestIdentifier,
            reason: String? = null,
        ): IntermediateTestResult {
            return IntermediateTestResult(
                testId = testIdentifier.uniqueId,
                parentId = testIdentifier.parentId.orElse(null),
                status = TestResult.Status.SKIPPED,
                displayName = testIdentifier.displayName.substringBeforeLast("("),
                tags = testIdentifier.tags.map { it.name }.toSet(),
                skippingReason = reason,
            )
        }
    }

    /**
     * Generates [TestResult] instance from this intermediate result.
     * @param children Children of this test or test container.
     * Should be set to `false` to transform roots.
     */
    @JvmSynthetic
    internal fun toTestResult(children: List<TestResult>): TestResult {
        val numberOfTests = when {
            children.isNotEmpty() -> children.sumOf { it.numberOfTests }
            else -> 1
        }
        val numberOfFailures = when {
            children.isNotEmpty() -> children.sumOf { it.numberOfFailures }
            else -> if (status == TestResult.Status.FAILED) 1 else 0
        }
        val numberOfSkipped = when {
            children.isNotEmpty() -> children.sumOf { it.numberOfSkipped }
            else -> if (status == TestResult.Status.SKIPPED) 1 else 0
        }
        if (status == TestResult.Status.SKIPPED) {
            return SkippedTestResult(
                testId = testId,
                parentId = parentId,
                children = children,
                status = status,
                numberOfTests = numberOfTests,
                numberOfFailures = numberOfFailures,
                numberOfSkipped = numberOfSkipped,
                displayName = displayName,
                tags = tags,
                reason = skippingReason,
            )
        } else {
            return FinishedTestResult(
                testId = testId,
                parentId = parentId,
                children = children,
                status = status,
                startTime = startTime!!,
                endTime = endTime!!,
                numberOfTests = numberOfTests,
                numberOfFailures = numberOfFailures,
                numberOfSkipped = numberOfSkipped,
                displayName = displayName,
                tags = tags,
                reportEntries = reportEntries!!,
                logs = logs!!,
                stats = stats!!,
                throwable = throwable,
            )
        }
    }

    override fun toString(): String = toJson(this)
}
