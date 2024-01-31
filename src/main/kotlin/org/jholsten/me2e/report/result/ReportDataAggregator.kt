package org.jholsten.me2e.report.result

import org.jholsten.me2e.Me2eTestConfigStorage
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.LogAggregator
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import org.jholsten.me2e.report.stats.StatsAggregator
import org.jholsten.me2e.report.result.mapper.ReportEntryMapper
import org.jholsten.me2e.report.result.model.*
import org.jholsten.me2e.report.result.model.IntermediateTestResult
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry
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
import kotlin.reflect.full.primaryConstructor

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
            val summary = IntermediateTestResult.started(testIdentifier)
            intermediateTestResults[testIdentifier.uniqueId] = summary
            if (testIdentifier.parentId.isPresent) {
                intermediateTestResults[testIdentifier.parentId.get()]!!.logs += logAggregator.collectTestRunnerLogs()
            }
            logger.info("Running test ${testIdentifier.displayName}...")
        }

        /**
         * Callback function which is executed when the execution of a test or test container has finished.
         * Collects relevant data for the test report and stores the intermediate execution result.
         * @param testIdentifier Identifier of the finished test or test container.
         * @param testExecutionResult Result of the execution for the supplied [testIdentifier].
         */
        @JvmSynthetic
        internal fun onTestFinished(testIdentifier: TestIdentifier, testExecutionResult: org.junit.platform.engine.TestExecutionResult) {
            logger.info("Executing test ${testIdentifier.displayName} finished.")
            val summary = intermediateTestResults[testIdentifier.uniqueId]!!
            summary.finished(
                testExecutionResult = testExecutionResult,
                reportEntries = collectedReportEntries,
                logs = logAggregator.collectTestRunnerLogs(),
            )
            collectedReportEntries.clear()
            if (testIdentifier.parentId.isPresent) {
                intermediateTestResults[testIdentifier.parentId.get()]!!.logs += summary.logs
            }
        }

        /**
         * Callback function which is executed when a test or test container has been skipped.
         * Stores relevant data for the test report.
         * @param testIdentifier Identifier of the skipped test or test container.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        internal fun onTestSkipped(testIdentifier: TestIdentifier, reason: String?) {
            IntermediateTestResult.skipped(testIdentifier, reason)
            collectedReportEntries.clear()
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
            logger.info("Aggregating test summaries...")
            val result = aggregateSummaries(testPlan)
            networkTraceAggregator.collectPackets(result.roots.filterIsInstance<FinishedTestResult>())
            collectContainerLogsAndStats(result.roots.filterIsInstance<FinishedTestResult>())
            generateReport(result)
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
         * TODO: Maybe include collecting logs and stats
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
         * Collects and assigns the logs and stats to the corresponding tests according to their timestamps.
         * As this data is collected by consumers during the test execution and delays can occur (e.g. if a container is currently
         * busy), the entries can only be assigned to the corresponding tests at the very end, after all tests have been executed.
         * The logs and stats of the test containers are also assigned to their children in order to capture all the data collected
         * in `@BeforeAll` and `@AfterAll` methods. This means that the first child receives all entries since the start of its
         * parent and the last child receives all entries recorded up to the end of its parent.
         */
        private fun collectContainerLogsAndStats(roots: List<FinishedTestResult>) {
            val logs = logAggregator.collectContainerLogs()
            val stats = statsAggregator.collectStats()
            for (test in roots) {
                matchLogsAndStatsToTest(test, logs, stats)
            }
        }

        /**
         * Matches logs and stats to the given test, if it is not a test container (i.e. it does not contain any children).
         * If it is a test container however, the logs and traces are assigned to its children.
         * @param test Test for which corresponding logs and traces are to be matched.
         * @param logs All logs captured from all Docker containers and the test runner.
         * @param stats All stats captured from all Docker containers.
         */
        private fun matchLogsAndStatsToTest(test: FinishedTestResult, logs: List<AggregatedLogEntry>, stats: List<AggregatedStatsEntry>) {
            test.logs = (test.logs + logs.findLogsBetween(test.startTime, test.endTime)).sortedBy { it.timestamp }
            test.stats = stats.findStatsBetween(test.startTime, test.endTime)

            val finishedChildren = test.children.filterIsInstance<FinishedTestResult>()
            for (child in finishedChildren) {
                matchLogsAndStatsToTest(child, logs, stats)
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

        /**
         * Generates test reports using the [ReportGenerator]s specified in the [org.jholsten.me2e.Me2eTestConfig].
         */
        private fun generateReport(result: TestExecutionResult) {
            val reportGeneratorClasses = Me2eTestConfigStorage.configAnnotation?.reportGenerators ?: arrayOf(HtmlReportGenerator::class)
            for (reportGeneratorClass in reportGeneratorClasses) {
                if (reportGeneratorClass.primaryConstructor == null) {
                    logger.error("Cannot use report generator $reportGeneratorClass, since it does not have a primary, no argument constructor.")
                    continue
                }
                try {
                    val reportGenerator = reportGeneratorClass.primaryConstructor!!.callBy(emptyMap())
                    logger.info("Using report generator $reportGeneratorClass to generate test report.")
                    reportGenerator.generate(result)
                } catch (e: Exception) {
                    logger.error("Exception occurred while trying to generate report using $reportGeneratorClass.", e)
                }
            }
        }

        /**
         * Extension function to find all logs for which the log's timestamp is between the
         * given start (inclusive) and end time (inclusive).
         */
        private fun List<AggregatedLogEntry>.findLogsBetween(start: Instant, end: Instant): List<AggregatedLogEntry> {
            return this.filter { it.timestamp in start..end }
        }

        /**
         * Extension function to find all stats for which the stat entry's timestamp is between the
         * given start (inclusive) and end time (inclusive).
         */
        private fun List<AggregatedStatsEntry>.findStatsBetween(start: Instant, end: Instant): List<AggregatedStatsEntry> {
            return this.filter { it.timestamp in start..end }
        }
    }
}
