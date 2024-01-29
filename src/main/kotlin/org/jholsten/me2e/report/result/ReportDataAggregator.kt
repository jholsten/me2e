package org.jholsten.me2e.report.result

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.LogAggregator
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import org.jholsten.me2e.report.stats.StatsAggregator
import org.jholsten.me2e.report.result.mapper.ReportEntryMapper
import org.jholsten.me2e.report.result.model.*
import org.jholsten.me2e.report.result.model.IntermediateTestResult
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
import org.jholsten.me2e.report.tracing.NetworkTraceAggregator
import org.jholsten.me2e.utils.logger
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.engine.support.descriptor.PackageSource
import org.junit.platform.engine.support.descriptor.UriSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

/**
 * Service which aggregates all the data required for the test report.
 */
class ReportDataAggregator private constructor() {
    companion object {
        private val logger = logger(this)

        /**
         * Service which represents the Test Runner.
         */
        @JvmSynthetic
        internal val testRunner: ServiceSpecification = ServiceSpecification(name = "Test Runner")

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
         * Network trace aggregator which collects HTTP packets from all Docker networks.
         */
        private val networkTraceAggregator: NetworkTraceAggregator = NetworkTraceAggregator()

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
            networkTraceAggregator.collectPackets(result.roots.filterIsInstance<FinishedTestResult>())
            HtmlReportGenerator().generate(result)
            println("TODO: ON TEST EXECUTION FINISHED")
        }

        /**
         * Callback function which is executed after the given Docker container was started.
         */
        @JvmSynthetic
        internal fun onContainerStarted(container: Container) {
            val specification = ServiceSpecification(name = container.name)
            logAggregator.onContainerStarted(container, specification)
            statsAggregator.onContainerStarted(container, specification)
            networkTraceAggregator.onContainerStarted(container, specification)
        }

        private fun aggregateSummaries(testPlan: TestPlan): TestExecutionResult {
            val roots = buildTestTree(testPlan)
            return TestExecutionResult(
                numberOfTests = roots.sumOf { it.numberOfTests },
                numberOfFailures = roots.sumOf { it.numberOfFailures },
                numberOfSkipped = roots.sumOf { it.numberOfSkipped },
                roots = roots,
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
                val source = getSource(root) ?: UUID.randomUUID().toString()
                val result = intermediateResult.toTestResult(
                    source = source,
                    parents = listOf(),
                    children = buildTestTree(source, listOf(intermediateResult), root, testPlan),
                )
                roots.add(result)
                moveTestContainerLogsAndStatsToChildren(result)
            }
            return roots
        }

        private fun buildTestTree(
            source: String,
            parents: List<IntermediateTestResult>,
            identifier: TestIdentifier,
            testPlan: TestPlan
        ): List<TestResult> {
            val nodes: MutableList<TestResult> = mutableListOf()
            val children = testPlan.getChildren(identifier)
            for (child in children) {
                val intermediateResult = getIntermediateResult(child)
                val result = intermediateResult.toTestResult(
                    source = source,
                    parents = parents,
                    children = buildTestTree(source, parents.toList() + intermediateResult, child, testPlan)
                )
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

        /**
         * Moves the logs and stats of the test containers to their children.
         * The first child receives all logs and stats that have been recorded since the start of the test container.
         * The last child receives all logs and stats that have been recorded up to the end of the test container.
         * As a result, all logs and stats are only assigned to individual tests and no longer to the test containers.
         * @param parentLogs Logs of the test's parent. Only set if [test] is its first or last child.
         * @param parentStats Stats of the test's parent. Only set if [test] is its first or last child.
         */
        private fun moveTestContainerLogsAndStatsToChildren(
            test: TestResult,
            parentLogs: List<AggregatedLogEntry>? = null,
            parentStats: List<AggregatedStatsEntry>? = null,
        ) {
            if (test !is FinishedTestResult) {
                return
            }
            val testLogs = parentLogs?.let { test.logs + it } ?: test.logs
            val testStats = parentStats?.let { test.stats + it } ?: test.stats
            if (test.children.isEmpty()) {
                test.logs = AggregatedLogEntryList(testLogs.sortedBy { it.timestamp })
                test.stats = AggregatedStatsEntryList(testStats.sortedBy { it.timestamp })
            } else {
                val finishedChildren = test.children.filterIsInstance<FinishedTestResult>()
                for ((index, child) in finishedChildren.withIndex()) {
                    var logs: List<AggregatedLogEntry>? = null
                    var stats: List<AggregatedStatsEntry>? = null
                    if (index == 0) {
                        logs = testLogs.filter { it.timestamp <= child.startTime }
                        stats = testStats.filter { it.timestamp <= child.startTime }
                    }
                    if (index == finishedChildren.size - 1) {
                        logs = testLogs.filter { it.timestamp >= child.startTime }
                        stats = testStats.filter { it.timestamp >= child.startTime }
                    }
                    moveTestContainerLogsAndStatsToChildren(child, logs, stats)
                }
                test.logs.clear()
                test.stats.clear()
            }
        }

        private fun getSource(testIdentifier: TestIdentifier): String? {
            val source = testIdentifier.source.getOrNull() ?: return null
            return when (source) {
                is ClassSource -> source.className
                is ClasspathResourceSource -> source.classpathResourceName
                is UriSource -> source.uri.toString()
                is MethodSource -> source.className
                is PackageSource -> source.packageName
                else -> null
            }
        }

        private fun storeIntermediateTestResult(result: IntermediateTestResult) {
            intermediateTestResults[result.testId] = result
            collectedReportEntries.clear()
        }
    }
}
