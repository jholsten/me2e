package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.result.utils.calculateSuccessRate

/**
 * Result of the execution of all tests.
 * Contains metrics about the number of tests and their duration
 * as well as the details of each test execution.
 */
class TestExecutionResult(
    /**
     * Total number of tests that were executed.
     */
    val numberOfTests: Int,

    /**
     * Total number of failed tests.
     */
    val numberOfFailures: Int,

    /**
     * Total number of tests which were skipped.
     */
    val numberOfSkipped: Int,

    /**
     * Roots of all tests that have been performed along with their detailed test results.
     * Typically, this list contains only one entry with the test engine `[engine:junit-jupiter]`,
     * whose children in turn contain the executed test classes and whose children contain the
     * executed tests. Along with nested classes and parameterized tests, this forms a tree of the
     * results of all executed tests, which may span over several levels.
     * @see org.junit.platform.launcher.TestPlan
     */
    val tests: List<TestResult>,
) {

    /**
     * Relative share of successful tests in the total number of tests, while excluding skipped tests.
     * Is set to `null` in case the result contains only skipped tests (i.e. [numberOfSkipped] is equal
     * to [numberOfTests]).
     */
    val successRate: Int? = calculateSuccessRate(numberOfTests, numberOfFailures, numberOfSkipped)
}
