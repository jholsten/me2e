package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.result.utils.calculateSuccessRate
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
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
     * Summaries of the children of this test of test container.
     * For instance, if this summary describes a Test Class, the children include all tests of this class.
     * For a leaf, this list is empty.
     */
    val children: List<TestSummary>,

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
        @JvmSynthetic
        internal fun fromIntermediateTestSummary(intermediateSummary: IntermediateTestSummary) {

        }
    }

    override fun toString(): String = toJson(this)
}
