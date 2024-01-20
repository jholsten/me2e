package org.jholsten.me2e.report

import com.google.auto.service.AutoService
import org.jholsten.me2e.report.summary.ReportDataAggregator
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

/**
 * Test executing listener which collects relevant data after each test execution
 * and generates the test report after all tests were executed.
 * This listener is automatically detected by JUnit.
 */
@AutoService(TestExecutionListener::class)
class ReportGeneratingListener : TestExecutionListener {
    /**
     * Callback function which is executed when the execution of a test or a test container has finished.
     * Is called after all [org.junit.jupiter.api.AfterEach] and [org.junit.jupiter.api.AfterAll] methods
     * have finished running.
     * Collects relevant data from the Docker containers for the test report and stores the execution result.
     * @param testIdentifier Identifier of the finished test or container.
     * @param testExecutionResult Result of the execution for the supplied [testIdentifier]
     */
    override fun executionFinished(testIdentifier: TestIdentifier?, testExecutionResult: TestExecutionResult?) {
        if (testIdentifier != null && testExecutionResult != null) {
            ReportDataAggregator.onTestFinished(testIdentifier, testExecutionResult)
        }
    }

    /**
     * Callback function which is executed when a test or a test container has been skipped.
     * Stores relevant data for the test report.
     * @param testIdentifier Identifier of the skipped test or container.
     * @param reason Message describing why the execution has been skipped.
     */
    override fun executionSkipped(testIdentifier: TestIdentifier?, reason: String?) {
        if (testIdentifier != null) {
            ReportDataAggregator.onTestSkipped(testIdentifier, reason)
        }
    }

    /**
     * Callback function which is executed when additional test reporting data has been published
     * via the [org.junit.jupiter.api.TestReporter].
     * Includes entry in test report.
     * @param testIdentifier Describes the test or container to which the entry pertains.
     * @param entry The published [ReportEntry].
     */
    override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
        if (testIdentifier != null && entry != null) {
            ReportDataAggregator.onReportingEntryPublished(entry)
        }
    }

    /**
     * Callback function which is executed before the execution of the tests starts.
     * Initializes all data collectors.
     * @param testPlan Describes the tree of tests about to be executed.
     */
    override fun testPlanExecutionStarted(testPlan: TestPlan?) {
        ReportDataAggregator.onTestExecutionStarted()
    }

    /**
     * Callback function which is executed after all tests have been executed.
     * Generates report using the [org.jholsten.me2e.report.summary.ReportGenerator].
     * @param testPlan Describes the tree of tests that have been executed.
     */
    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        ReportDataAggregator.onTestExecutionFinished(testPlan)
    }
}
