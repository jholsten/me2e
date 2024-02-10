package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.result.mapper.TestSummaryStatusMapper
import org.jholsten.me2e.utils.toJson
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestIdentifier
import java.time.Instant

/**
 * Intermediate summary of the execution of a single test or test container.
 * Contains all data for a test and its result during the test execution. As certain information can only be
 * determined after all tests have been executed, this model is converted into a [TestResult] after the execution
 * of all tests has finished.
 */
internal class IntermediateTestResult(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    private val testId: String,

    /**
     * ID of the parent of this test or test container.
     * An identifier without a parent is called a `root`.
     * @see org.junit.platform.launcher.TestIdentifier.getParentId
     */
    @JvmSynthetic
    var parentId: String?,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    private var status: TestResult.Status? = null,

    /**
     * Timestamp of when this test or test container has started its execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    private val startTime: Instant? = null,

    /**
     * Timestamp of when this test or test container has finished its execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    private var endTime: Instant? = null,

    /**
     * Human-readable name of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getDisplayName
     */
    private val displayName: String,

    /**
     * Tags associated with this test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getTags
     */
    private val tags: Set<String>,

    /**
     * Additional report entries that were published during the execution of this test or test container.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    @JvmSynthetic
    var reportEntries: MutableList<ReportEntry> = mutableListOf(),

    /**
     * Logs from the Test Runner that were collected for this test execution.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     */
    @JvmSynthetic
    val logs: MutableList<AggregatedLogEntry> = mutableListOf(),

    /**
     * Throwable that caused this result.
     * Only set for tests for which the status is not [TestResult.Status.SKIPPED].
     * @see org.junit.platform.engine.TestExecutionResult.getThrowable
     */
    private var throwable: Throwable? = null,

    /**
     * Message describing why the execution has been skipped.
     * Only set for tests with status [TestResult.Status.SKIPPED].
     */
    private var skippingReason: String? = null,
) {
    companion object {
        /**
         * Creates and instance of [IntermediateTestResult] representing a test or test container for which the
         * execution has just been started. Parses information from the given identifier supplied by JUnit and
         * sets the start time of the test to the current timestamp.
         * @param testIdentifier Identifier of the started test.
         */
        @JvmSynthetic
        fun started(testIdentifier: TestIdentifier): IntermediateTestResult {
            return IntermediateTestResult(
                testId = testIdentifier.uniqueId,
                parentId = testIdentifier.parentId.orElse(null),
                startTime = Instant.now(),
                displayName = testIdentifier.displayName.substringBeforeLast("("),
                tags = testIdentifier.tags.map { it.name }.toSet(),
            )
        }

        /**
         * Creates an instance of [IntermediateTestResult] representing a test or test container for which the
         * execution was skipped. Parses information from the given identifier supplied by JUnit and sets the
         * reason of why the execution has been skipped.
         * @param testIdentifier Identifier of the skipped test.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        fun skipped(testIdentifier: TestIdentifier, reason: String? = null): IntermediateTestResult {
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
     * Marks the test represented by this instance as finished. Sets the provided additional information which
     * are available only after the execution has finished.
     * @param testExecutionResult Result of the execution for this test or test container.
     * @param reportEntries Report entries that were published during the execution of this test or test container.
     * @param logs Logs which were captured during the execution of this test or test container.
     */
    @JvmSynthetic
    fun finished(testExecutionResult: TestExecutionResult, reportEntries: List<ReportEntry>, logs: List<AggregatedLogEntry>) {
        this.status = TestSummaryStatusMapper.INSTANCE.toInternalDto(testExecutionResult.status)
        this.endTime = Instant.now()
        this.reportEntries = reportEntries.toMutableList()
        this.logs += logs
        this.throwable = testExecutionResult.throwable.orElse(null)
    }

    /**
     * Generates [TestResult] instance from this intermediate result with the provided information.
     * @param source Source where this test or test container is defined.
     * @param parents Parents of this result in the overall test execution tree.
     * @param children Children of this test or test container.
     */
    @JvmSynthetic
    fun toTestResult(source: String, parents: List<IntermediateTestResult>, children: List<TestResult>): TestResult {
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
        val numberOfAborted = when {
            children.isNotEmpty() -> children.sumOf { it.numberOfAborted }
            else -> if (status == TestResult.Status.ABORTED) 1 else 0
        }
        val status = when {
            children.any { it.status == TestResult.Status.FAILED } -> TestResult.Status.FAILED
            else -> status
        }
        val path = parents.map { it.testId to it.displayName } + Pair(testId, displayName)
        if (status == TestResult.Status.SKIPPED) {
            val reason = when {
                skippingReason != null -> skippingReason
                else -> parents.find { it.status == TestResult.Status.SKIPPED }?.skippingReason
            }
            return SkippedTestResult(
                testId = testId,
                source = source,
                path = path,
                parentId = parentId,
                children = children,
                status = status,
                numberOfTests = numberOfTests,
                numberOfFailures = numberOfFailures,
                numberOfSkipped = numberOfSkipped,
                numberOfAborted = numberOfAborted,
                displayName = displayName,
                tags = tags,
                reason = reason,
            )
        } else {
            return FinishedTestResult(
                testId = testId,
                source = source,
                path = path,
                parentId = parentId,
                children = children,
                status = status!!,
                startTime = startTime!!,
                endTime = endTime!!,
                numberOfTests = numberOfTests,
                numberOfFailures = numberOfFailures,
                numberOfSkipped = numberOfSkipped,
                numberOfAborted = numberOfAborted,
                displayName = displayName,
                tags = tags,
                reportEntries = reportEntries,
                logs = logs,
                throwable = throwable,
            )
        }
    }

    override fun toString(): String = toJson(this)
}
