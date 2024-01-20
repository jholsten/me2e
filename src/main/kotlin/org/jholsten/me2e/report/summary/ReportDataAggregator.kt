package org.jholsten.me2e.report.summary

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.LogAggregator
import org.jholsten.me2e.report.stats.StatsAggregator
import org.jholsten.me2e.report.summary.mapper.ReportEntryMapper
import org.jholsten.me2e.report.summary.model.ReportEntry
import org.jholsten.me2e.report.summary.model.TestSummary
import org.jholsten.me2e.utils.logger
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.time.Instant

/**
 * Service which aggregates all the data required for the test report.
 */
class ReportDataAggregator private constructor() {
    companion object {
        private val logger = logger(this)

        /**
         * Log collector which aggregates the logs of all containers for each test execution.
         */
        private val logAggregator: LogAggregator = LogAggregator()

        /**
         * Statistics collector which aggregates the resource usage statistics of all containers
         * for each test execution.
         */
        private val statsAggregator: StatsAggregator = StatsAggregator()

        /**
         * Summaries of all tests and test containers executed so far.
         */
        private val testSummaries: MutableList<TestSummary> = mutableListOf()

        /**
         * Report entries that were collected so far for the current test execution.
         */
        private val collectedReportEntries: MutableList<ReportEntry> = mutableListOf()

        /**
         * Timestamps of when a test or test container was started as map of test ID and timestamp.
         */
        private val startTimes: MutableMap<String, Instant> = mutableMapOf()

        @JvmSynthetic
        internal fun onTestExecutionStarted() {
            logAggregator.initializeOnTestExecutionStarted()
        }

        /**
         * Initializes the aggregator when the containers were started.
         * Starts listeners for Container events.
         */
        @JvmSynthetic
        internal fun initializeOnContainersStarted(containers: Collection<Container>) {
            logAggregator.initializeOnContainersStarted(containers)
            statsAggregator.initializeOnContainersStarted(containers)
        }

        /**
         * Callback function which is executed when the execution of a test or test container is about to be started.
         * Records the start time of the test for the test report.
         * @param testIdentifier Identifier of the test or test container about to be started.
         */
        @JvmSynthetic
        internal fun onTestStarted(testIdentifier: TestIdentifier) {
            startTimes[testIdentifier.uniqueId] = Instant.now()
        }

        /**
         * Callback function which is executed when the execution of a test or test container has finished.
         * Collects relevant data from the Docker containers for the test report and stores the execution result.
         * @param testIdentifier Identifier of the finished test or test container.
         * @param testExecutionResult Result of the execution for the supplied [testIdentifier].
         */
        @JvmSynthetic
        internal fun onTestFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
            val logs = logAggregator.collectLogs(testIdentifier.uniqueId)
            val stats = statsAggregator.collectStats(testIdentifier.uniqueId)
            val summary = TestSummary.finished(
                testIdentifier = testIdentifier,
                testExecutionResult = testExecutionResult,
                startTime = startTimes[testIdentifier.uniqueId],
                reportEntries = collectedReportEntries,
                logs = logs,
                stats = stats,
            )
            storeTestSummary(summary)
        }

        /**
         * Callback function which is executed when a test or test container has been skipped.
         * Stores relevant data for the test report.
         * @param testIdentifier Identifier of the skipped test or test container.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        internal fun onTestSkipped(testIdentifier: TestIdentifier, reason: String?) {
            val summary = TestSummary.skipped(testIdentifier, reason)
            storeTestSummary(summary)
        }

        /**
         * Callback function which is executed when additional test reporting data has been published
         * via the [org.junit.jupiter.api.TestReporter].
         * Includes entry in test report.
         * @param entry The published [ReportEntry].
         */
        @JvmSynthetic
        internal fun onReportingEntryPublished(entry: org.junit.platform.engine.reporting.ReportEntry) {
            collectedReportEntries.add(ReportEntryMapper.INSTANCE.toInternalDto(entry))
        }

        /**
         * Callback function which is executed after all tests have been finished.
         * Generates report using the [org.jholsten.me2e.report.summary.ReportGenerator].
         * @param testPlan Describes the tree of tests that have been executed.
         */
        @JvmSynthetic
        internal fun onTestExecutionFinished(testPlan: TestPlan?) {
            val logs = logAggregator.getAggregatedLogs()
            val stats = statsAggregator.getAggregatedStats()
            println("TODO: ON TEST EXECUTION FINISHED")
        }

        private fun storeTestSummary(summary: TestSummary) {
            testSummaries.add(summary)
            collectedReportEntries.clear()
        }
    }
}
