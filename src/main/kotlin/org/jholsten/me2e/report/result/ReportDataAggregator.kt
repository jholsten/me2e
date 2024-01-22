package org.jholsten.me2e.report.result

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.LogAggregator
import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import org.jholsten.me2e.report.stats.StatsAggregator
import org.jholsten.me2e.report.result.mapper.ReportEntryMapper
import org.jholsten.me2e.report.result.model.IntermediateTestResult
import org.jholsten.me2e.report.result.model.ReportEntry
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.utils.logger
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
        private val intermediateTestResults: MutableMap<String, IntermediateTestResult> = mutableMapOf()

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
        internal fun onTestFinished(testIdentifier: TestIdentifier, testExecutionResult: org.junit.platform.engine.TestExecutionResult) {
            val logs = logAggregator.collectLogs(testIdentifier.uniqueId)
            val stats = statsAggregator.collectStats(testIdentifier.uniqueId)
            val summary = IntermediateTestResult.finished(
                testIdentifier = testIdentifier,
                testExecutionResult = testExecutionResult,
                startTime = startTimes[testIdentifier.uniqueId],
                reportEntries = collectedReportEntries,
                logs = logs,
                stats = stats,
            )
            storeIntermediateTestResult(summary)
        }

        /**
         * Callback function which is executed when a test or test container has been skipped.
         * Stores relevant data for the test report.
         * @param testIdentifier Identifier of the skipped test or test container.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        internal fun onTestSkipped(testIdentifier: TestIdentifier, reason: String?) {
            val summary = IntermediateTestResult.skipped(testIdentifier, reason)
            storeIntermediateTestResult(summary)
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
         * Generates report using the [org.jholsten.me2e.report.result.ReportGenerator].
         * @param testPlan Describes the tree of tests that have been executed.
         */
        @JvmSynthetic
        internal fun onTestExecutionFinished(testPlan: TestPlan) {
            val logs = logAggregator.getAggregatedLogs()
            val stats = statsAggregator.getAggregatedStats()
            val result = aggregateSummaries(testPlan)
            HtmlReportGenerator().generate(result)
            println("TODO: ON TEST EXECUTION FINISHED")
        }

        private fun aggregateSummaries(testPlan: TestPlan): TestExecutionResult {
            val tree = buildTestTree(testPlan)
            return TestExecutionResult(
                numberOfTests = tree.sumOf { it.numberOfTests },
                numberOfFailures = tree.sumOf { it.numberOfFailures },
                numberOfSkipped = tree.sumOf { it.numberOfSkipped },
                tests = tree,
            )
        }

        /**
         * Builds tree of the test containers and tests which were executed.
         * The original roots of the test plan, i.e. the test engine such as [engine:junit-jupiter]`,
         * are not included in the tree. Instead, the roots of the tree to be built are formed by
         * the underlying children, which are typically the executed test classes.
         */
        private fun buildTestTree(testPlan: TestPlan): List<TestResult> {
            val roots: MutableList<TestResult> = mutableListOf()
            val flattened = testPlan.roots.flatMap { testPlan.getChildren(it) }
            for (root in flattened) {
                val intermediateResult = getIntermediateResult(root)
                intermediateResult.parentId = null
                val result = intermediateResult.toTestResult(buildTestTree(root, testPlan))
                roots.add(result)
            }
            return roots
        }

        private fun buildTestTree(identifier: TestIdentifier, testPlan: TestPlan): List<TestResult> {
            val nodes: MutableList<TestResult> = mutableListOf()
            val children = testPlan.getChildren(identifier)
            for (child in children) {
                val result = getIntermediateResult(child).toTestResult(buildTestTree(child, testPlan))
                nodes.add(result)
            }
            return nodes
        }

        /**
         * Returns the intermediate test result which is stored for the given identifier.
         * If no such result is saved, we assume that the enclosing test container was
         * skipped and therefore all the tests it contains were not executed.
         * This situation and the case where the initialization of a test container has
         * failed are the only cases in which it is possible that no result is stored
         * for the identifier.
         */
        private fun getIntermediateResult(testIdentifier: TestIdentifier): IntermediateTestResult {
            return this.intermediateTestResults[testIdentifier.uniqueId] ?: IntermediateTestResult.skipped(testIdentifier)
        }

        private fun storeIntermediateTestResult(result: IntermediateTestResult) {
            intermediateTestResults[result.testId] = result
            collectedReportEntries.clear()
        }
    }
}
