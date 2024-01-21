package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.utils.toJson

/**
 * Summary of a test or test container for which the execution was skipped.
 */
class SkippedTestSummary(
    /**
     * Unique identifier of the test or test container.
     * @see org.junit.platform.launcher.TestIdentifier.getUniqueId
     */
    testId: String,

    /**
     * Summaries of the children of this test of test container.
     * For instance, if this summary describes a Test Class, the children include all tests of this class.
     * For a leaf, this list is empty.
     */
    children: List<TestSummary>,

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
     * Message describing why the execution has been skipped.
     */
    val reason: String?,
) : TestSummary(
    testId = testId,
    children = children,
    status = status,
    displayName = displayName,
    tags = tags,
) {
    override fun toString(): String = toJson(this)
}
