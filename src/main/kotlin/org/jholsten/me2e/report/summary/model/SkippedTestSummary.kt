package org.jholsten.me2e.report.summary.model

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
     * Message describing why the execution has been skipped.
     */
    val reason: String?,
) : TestSummary(
    testId = testId,
    parentId = parentId,
    status = status,
    displayName = displayName,
    tags = tags,
) {
    override fun toString(): String = toJson(this)
}
