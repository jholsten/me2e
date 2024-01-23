package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.result.utils.calculateSuccessRate
import org.jholsten.me2e.utils.toJson

/**
 * Summary of the execution of a single test or test container.
 * A test container may be for example a Test Class, [org.junit.jupiter.api.Nested] Class
 * or a [org.junit.jupiter.params.ParameterizedTest].
 */
abstract class TestResult(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    val testId: String,

    /**
     * Path of this result in the overall test execution tree from the root to this test.
     * Contains the test ID along with the display name of the tests and test containers.
     */
    val path: List<Pair<String, String>>,

    /**
     * ID of the parent of this test or test container.
     * An identifier without a parent is called a `root`.
     * @see org.junit.platform.launcher.TestIdentifier.getParentId
     */
    val parentId: String?,

    /**
     * Summaries of the children of this test or test container.
     * For instance, if this summary describes a Test Class, the children include all tests of this class.
     * For a leaf, this list is empty.
     */
    val children: List<TestResult>,

    /**
     * Status of the test execution.
     * @see org.junit.platform.engine.TestExecutionResult.getStatus
     */
    val status: Status,

    /**
     * Number of tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `1`.
     * However, if this result relates to a test container, the value
     * reflects the number of tests contained in the container.
     */
    val numberOfTests: Int,

    /**
     * Number of failed tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `0` or `1`, depending on the status.
     * However, if this result relates to a test container, the value
     * reflects the number of failed tests contained in the container.
     */
    val numberOfFailures: Int,

    /**
     * Number of skipped tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `0` or `1`, depending on the status.
     * However, if this result relates to a test container, the value
     * reflects the number of skipped tests contained in the container.
     */
    val numberOfSkipped: Int,

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
     * Relative share of successful tests in the total number of tests, while excluding skipped tests.
     * Is set to `null` in case the result contains only skipped tests (i.e. [numberOfSkipped] is equal
     * to [numberOfTests]).
     */
    val successRate: Int? = calculateSuccessRate(numberOfTests, numberOfFailures, numberOfSkipped)

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

    override fun toString(): String = toJson(this)
}
