package org.jholsten.me2e.report.summary.model

import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.summary.mapper.TestSummaryStatusMapper
import org.jholsten.me2e.utils.toJson
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestIdentifier
import java.time.Instant

/**
 * Summary of the execution of a single test or test container.
 * A test container may be for example a Test Class, [org.junit.jupiter.api.Nested] Class or a [org.junit.jupiter.params.ParameterizedTest].
 * At the root of all Test Classes is the test container with id `[engine:junit-jupiter]`.
 */
abstract class TestSummary(
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
    val parentId: String?,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    val status: Status,

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
) {
    /**
     * Status of executing a single test or test container.
     * @see org.junit.platform.engine.TestExecutionResult.Status
     */
    enum class Status {
        /**
         * Indicates that the execution of the test or test container was successful.
         */
        SUCCESSFUL,

        /**
         * Indicates that the execution of the test or test container was aborted (started but not finished).
         */
        ABORTED,

        /**
         * Indicates that the execution of the test or test container failed.
         */
        FAILED,

        /**
         * Indicates that the execution of the test or test container was skipped.
         */
        SKIPPED,
    }

    companion object {
        /**
         * Creates an instance of [FinishedTestSummary] representing a test or test container for which the execution was finished.
         * @param testIdentifier Identifier of the finished test or test container.
         * @param testExecutionResult Result of the execution for the supplied [testIdentifier].
         * @param startTime Timestamp of when this test or test container has started its execution.
         * @param reportEntries Report entries that were published during the test execution.
         * @param logs Logs that were collected during the test execution.
         */
        @JvmSynthetic
        internal fun finished(
            testIdentifier: TestIdentifier,
            testExecutionResult: TestExecutionResult,
            startTime: Instant?,
            reportEntries: List<ReportEntry>,
            logs: AggregatedLogEntryList,
        ): FinishedTestSummary {
            return FinishedTestSummary(
                testId = testIdentifier.uniqueId,
                parentId = testIdentifier.parentId.orElse(null),
                status = TestSummaryStatusMapper.INSTANCE.toInternalDto(testExecutionResult.status),
                startTime = startTime ?: Instant.now(),
                endTime = Instant.now(),
                displayName = testIdentifier.displayName,
                tags = testIdentifier.tags.map { it.name }.toSet(),
                reportEntries = reportEntries.toList(),
                logs = logs,
                throwable = testExecutionResult.throwable.orElse(null),
            )
        }

        /**
         * Creates an instance of [SkippedTestSummary] representing a test or test container for which the execution was skipped.
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
                parentId = testIdentifier.parentId.orElse(null),
                status = Status.SKIPPED,
                displayName = testIdentifier.displayName,
                tags = testIdentifier.tags.map { it.name }.toSet(),
                reason = reason,
            )
        }
    }

    override fun toString(): String = toJson(this)
}
