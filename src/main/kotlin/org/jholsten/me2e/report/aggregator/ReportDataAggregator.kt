package org.jholsten.me2e.report.aggregator

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.LogAggregator
import org.jholsten.me2e.report.summary.TestSummary
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

/**
 * Service which aggregates all the data required for the test report.
 */
class ReportDataAggregator {

    /**
     * Log collector which aggregates the logs of all containers for each test execution.
     */
    lateinit var logAggregator: LogAggregator

    private val testSummaries: MutableMap<String, TestSummary> = mutableMapOf()

    @JvmSynthetic
    internal fun initialize(containers: Collection<Container>) {
        logAggregator = LogAggregator(containers)
    }

    /**
     * Callback function which is executed when the execution of a test has finished.
     * Collects relevant data from the Docker containers for the test report and stores the execution result.
     * @param testIdentifier Identifier of the finished test.
     * @param testExecutionResult Result of the execution for the supplied [testIdentifier]
     */
    @JvmSynthetic
    internal fun onTestFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult?) {
        logAggregator.collectLogs(testIdentifier.uniqueId)
        val summary = testSummaries[testIdentifier.uniqueId] ?: TestSummary(
            testId = testIdentifier.uniqueId,
            status = TestSummary.Status.SUCCESSFUL, //TODO
            displayName = testIdentifier.displayName,
            tags = testIdentifier.tags.map { it.name }.toSet(),
            reportEntries = listOf(), // TODO
            logs = logAggregator.getAggregatedLogsByTestId(testIdentifier.uniqueId),
        )
        println("TODO: ON TEST FINISHED")
    }

    /**
     * Callback function which is executed when a test has been skipped.
     * Stores relevant data for the test report.
     * @param testIdentifier Identifier of the skipped test or container.
     * @param reason Message describing why the execution has been skipped.
     */
    @JvmSynthetic
    internal fun onTestSkipped(testIdentifier: TestIdentifier, reason: String?) {
        println("TODO: ON TEST SKIPPED")
    }

    /**
     * Callback function which is executed when additional test reporting data has been published
     * via the [org.junit.jupiter.api.TestReporter].
     * Includes entry in test report.
     * @param testIdentifier Describes the test or container to which the entry pertains.
     * @param entry The published [ReportEntry].
     */
    @JvmSynthetic
    internal fun onReportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        println("TODO: ON REPORTING ENTRY PUBLISHED")
    }

    /**
     * Callback function which is executed after all tests have been executed.
     * Generates report using the [org.jholsten.me2e.report.summary.ReportGenerator].
     * @param testPlan Describes the tree of tests that have been executed.
     */
    @JvmSynthetic
    internal fun onTestExecutionFinished(testPlan: TestPlan?) {
        println("TODO: ON TEST EXECUTION FINISHED")
    }
}
