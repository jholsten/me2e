package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.utils.toJson

/**
 * Summary of a test or test container for which the execution was skipped.
 */
class SkippedTestResult(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    testId: String,

    /**
     * Source of the [TestExecutionResult.roots] where this test or test container is defined.
     * Typically, this represents the name of the surrounding test class.
     * @see org.junit.platform.launcher.TestIdentifier.getSource
     */
    source: String,

    /**
     * Path of this result in the overall test execution tree from the root to this test.
     * Contains the test ID along with the display name of the tests and test containers.
     */
    path: List<Pair<String, String>>,

    /**
     * ID of the parent of this test or test container.
     * An identifier without a parent is called a `root`.
     * @see org.junit.platform.launcher.TestIdentifier.getParentId
     */
    parentId: String?,

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
     * Number of tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `1`.
     * However, if this result relates to a test container, the value
     * reflects the number of tests contained in the container.
     */
    numberOfTests: Int,

    /**
     * Number of failed tests that this result contains.
     * Since this result relates to a skipped test or test container and
     * therefore all of its [children] were skipped as well, this value
     * is set to `0`.
     */
    numberOfFailures: Int,

    /**
     * Number of skipped tests that this result contains.
     * If this result relates to a single test (i.e. the list of [children]
     * is empty), the value is set to `1`.
     * However, if this result relates to a test container, the value
     * reflects the number of skipped tests contained in the container.
     */
    numberOfSkipped: Int,

    /**
     * Number of aborted tests that this result contains.
     * Since this result relates to a skipped test or test container and
     * therefore all of its [children] were skipped as well, this value
     * is set to `0`.
     */
    numberOfAborted: Int,

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
     * Message describing why the execution has been skipped.
     */
    val reason: String?,
) : TestResult(
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
) {
    override fun toString(): String = toJson(this)
}
