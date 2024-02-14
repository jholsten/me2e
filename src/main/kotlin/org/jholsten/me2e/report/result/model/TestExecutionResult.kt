package org.jholsten.me2e.report.result.model

import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.result.utils.calculateDurationInSeconds
import org.jholsten.me2e.report.result.utils.calculateSuccessRate
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry
import java.math.BigDecimal
import java.time.Instant

/**
 * Result of the execution of all tests.
 * Contains metrics about the number of tests and their duration as well as the details of each test execution.
 */
class TestExecutionResult internal constructor(
    /**
     * Timestamp of when the test execution has started.
     */
    val startTime: Instant,

    /**
     * Timestamp of when the test execution has finished.
     */
    val endTime: Instant,

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
     * Total number of tests which were aborted.
     */
    val numberOfAborted: Int,

    /**
     * Logs that were collected for the execution of all tests.
     * Includes Test Runner logs as well as Docker container logs.
     */
    val logs: List<AggregatedLogEntry>,

    /**
     * Resource usage statistics of all Docker containers that were collected for
     * the execution of all tests.
     */
    val stats: List<AggregatedStatsEntry>,

    /**
     * Roots of all tests included in this result along with their detailed test results.
     * Typically, this list contains the executed test classes, whose children in turn contain
     * the executed tests. Along with nested classes and parameterized tests, this forms a tree
     * of the results of all executed tests, which may span over several levels.
     * @see org.junit.platform.launcher.TestPlan
     */
    val roots: List<TestResult>,
) {

    /**
     * Relative share of successful tests in the total number of tests, while excluding skipped tests.
     * Is set to `null` in case the result contains only skipped tests (i.e. [numberOfSkipped] is equal
     * to [numberOfTests]).
     */
    val successRate: Int? = calculateSuccessRate(numberOfTests, numberOfFailures, numberOfSkipped)

    /**
     * Number of seconds that executing all tests took.
     * Includes the duration it took to start and stop the test environment.
     */
    val duration: BigDecimal = calculateDurationInSeconds(startTime, endTime)
}
