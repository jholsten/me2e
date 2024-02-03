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
import org.junit.platform.engine.reporting.ReportEntry as JUnitReportEntry
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.ClasspathResourceSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.engine.support.descriptor.PackageSource
import org.junit.platform.engine.support.descriptor.UriSource
import org.junit.platform.engine.TestExecutionResult as JUnitExecutionResult
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.time.Instant
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Service which collects and aggregates all the data required for the test report.
 * Collects logs of the test runner and Docker containers defined in the environment, the resource
 * usage statistics of the containers and the HTTP requests that were sent to the containers.
 * Once all tests have been executed, the collected data is aggregated and transferred to the
 * [ReportGenerator] to generate the test report.
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
         * Log collector which collects the logs of the test runner and of all Docker containers.
         */
        private val logAggregator: LogAggregator = LogAggregator()

        /**
         * Statistics collector which collects the resource usage statistics of all Docker containers.
         */
        private val statsAggregator: StatsAggregator = StatsAggregator()

        /**
         * Network trace aggregator which collects HTTP requests and responses from all Docker networks.
         */
        private val networkTraceAggregator: NetworkTraceAggregator = NetworkTraceAggregator()

        /**
         * Summaries of all tests and test containers (e.g. test classes or parameterized tests) executed so far.
         * At the end of the execution of all tests, this data is supplemented by the information contained in
         * the [TestPlan] and transferred to [TestResult] instances.
         */
        private val intermediateTestResults: MutableMap<String, IntermediateTestResult> = mutableMapOf()

        /**
         * Report entries that were collected so far for the current test execution.
         * As soon as a new report entry is published, the callback function [onReportingEntryPublished] is called,
         * which saves the published entry in this list. Once the execution of the test is complete, the collected
         * entries are assigned to the test and stored in the [IntermediateTestResult].
         */
        private val collectedReportEntries: MutableList<ReportEntry> = mutableListOf()

        /**
         * Callback function which is executed when the execution of the tests is about to be started.
         * Initializes [LogAggregator] to consume the Test Runner's logs.
         */
        @JvmSynthetic
        internal fun onTestExecutionStarted() {
            logAggregator.initializeOnTestExecutionStarted()
        }

        /**
         * Callback function which is executed when the execution of a test or test container is about to be started.
         * First, an [IntermediateTestResult] is saved for the test to be started, which contains general information
         * about the test, such as its name and ID. The start time of the test is also recorded.
         * In addition, the logs and report entries collected so far are assigned to the parent of this test. This
         * includes all log and report entries that were logged in a `@BeforeAll` method of a test class.
         * @param testIdentifier Identifier of the test or test container about to be started.
         */
        @JvmSynthetic
        internal fun onTestStarted(testIdentifier: TestIdentifier) {
            intermediateTestResults[testIdentifier.uniqueId] = IntermediateTestResult.started(testIdentifier)
            if (testIdentifier.parentId.isPresent) {
                intermediateTestResults[testIdentifier.parentId.get()]!!.logs += logAggregator.collectTestRunnerLogs()
                intermediateTestResults[testIdentifier.parentId.get()]!!.reportEntries += collectReportEntries()
            }
        }

        /**
         * Callback function which is executed when the execution of a test or test container has finished.
         * Stores all collected report entries as well as the entries logged by the Test Runner in the intermediate
         * execution result. In addition, the logs and report entries from this test are also saved in the parent's
         * intermediate result, so that it eventually contains all the logs and report entries that were recorded in
         * `@BeforeAll` and `@AfterAll` methods, as well as during the execution of all tests contained in the test container.
         * @param testIdentifier Identifier of the finished test or test container.
         * @param testExecutionResult Result of the execution for the supplied [testIdentifier].
         */
        @JvmSynthetic
        internal fun onTestFinished(testIdentifier: TestIdentifier, testExecutionResult: JUnitExecutionResult) {
            val summary = intermediateTestResults[testIdentifier.uniqueId]!!
            summary.finished(testExecutionResult, collectReportEntries(), logAggregator.collectTestRunnerLogs())
            if (testIdentifier.parentId.isPresent) {
                intermediateTestResults[testIdentifier.parentId.get()]!!.logs += summary.logs
                intermediateTestResults[testIdentifier.parentId.get()]!!.reportEntries += summary.reportEntries
            }
        }

        /**
         * Callback function which is executed when a test or test container has been skipped.
         * For a skipped test, [onTestStarted] and [onTestFinished] are not called, so that the intermediate execution
         * result is stored directly at this point with the information relevant for the report.
         * @param testIdentifier Identifier of the skipped test or test container.
         * @param reason Message describing why the execution has been skipped.
         */
        @JvmSynthetic
        internal fun onTestSkipped(testIdentifier: TestIdentifier, reason: String?) {
            val summary = IntermediateTestResult.skipped(testIdentifier, reason)
            intermediateTestResults[testIdentifier.uniqueId] = summary
        }

        /**
         * Callback function which is executed when additional test reporting data has been published
         * via the [org.junit.jupiter.api.TestReporter]. Stores entry for the currently executed test.
         * @param entry The published [ReportEntry].
         */
        @JvmSynthetic
        internal fun onReportingEntryPublished(entry: JUnitReportEntry) {
            collectedReportEntries.add(ReportEntryMapper.INSTANCE.toInternalDto(entry))
        }

        /**
         * Callback function which is executed after all tests have been finished.
         * Aggregates data from the intermediate execution results and the given test plan and generates
         * reports using the [ReportGenerator] defined in the [org.jholsten.me2e.Me2eTestConfig].
         * @param testPlan Describes the tree of tests that have been executed.
         */
        @JvmSynthetic
        internal fun onTestExecutionFinished(testPlan: TestPlan) {
            logger.info("Aggregating test summaries...")
            val result = aggregateSummaries(testPlan)
            networkTraceAggregator.collectPackets(result.roots.filterIsInstance<FinishedTestResult>())
            collectContainerLogsAndStats(result.roots.filterIsInstance<FinishedTestResult>())
            generateReports(result)
        }

        /**
         * Callback function which is executed after the given Docker container was started.
         * Initializes the collectors which collect relevant data from the containers.
         */
        @JvmSynthetic
        internal fun onContainerStarted(container: Container) {
            val specification = ServiceSpecification(name = container.name)
            logAggregator.onContainerStarted(container, specification)
            statsAggregator.onContainerStarted(container, specification)
            networkTraceAggregator.onContainerStarted(container, specification)
        }

        /**
         * Aggregates the intermediate execution results saved before and after each test execution and
         * supplements them with the information from the given test plan. The result contains a tree of
         * all tests, which is organized by test containers and their subordinate tests.
         * @param testPlan Describes the tree of tests that have been executed.
         * @return Aggregated result of the test execution.
         */
        private fun aggregateSummaries(testPlan: TestPlan): TestExecutionResult {
            val roots = buildTestTree(testPlan)
            return TestExecutionResult(
                numberOfTests = roots.sumOf { it.numberOfTests },
                numberOfFailures = roots.sumOf { it.numberOfFailures },
                numberOfSkipped = roots.sumOf { it.numberOfSkipped },
                numberOfAborted = roots.sumOf { it.numberOfAborted },
                roots = roots,
            )
        }

        /**
         * Builds tree of the test containers and tests which were executed.
         * The original roots of the test plan, i.e. the test engine such as `[engine:junit-jupiter]`,
         * are not included in the tree. Instead, the roots of the tree to be built are formed by
         * the underlying children, which are typically the executed test classes.
         * @param testPlan Describes the tree of tests that have been executed.
         * @return Aggregated roots of the test tree.
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

        /**
         * Builds test tree starting from the given [identifier]. The result contains the identifier's children.
         * @param source Source where the test or test container is defined, i.e. the path to the test class.
         * @param parents Parents of the [identifier].
         * @param identifier Test for which children are to be retrieved.
         * @param testPlan Describes the tree of tests that have been executed.
         * @return Aggregated children of the [identifier].
         */
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
         * Returns the intermediate test result which is stored for the given test identifier.
         * If no such result is saved, we assume that the enclosing test container was skipped and therefore all the tests
         * it contains were not executed. This situation and the case where the initialization of a test  container has
         * failed are the only cases in which it is possible that no result is stored for the identifier.
         * @param testIdentifier Test or test container for which intermediate result is to be retrieved.
         */
        private fun getIntermediateResult(testIdentifier: TestIdentifier): IntermediateTestResult {
            return this.intermediateTestResults[testIdentifier.uniqueId] ?: IntermediateTestResult.skipped(testIdentifier)
        }

        /**
         * Collects and assigns the Docker container's logs and stats to the corresponding tests according to their timestamps.
         * As this data is collected asynchronously by consumers during the test execution and delays can occur (e.g. if a
         * container is currently busy), the entries can only be assigned to the corresponding tests at the very end, after all
         * tests have been executed.
         * @param roots Aggregated roots of the test tree.
         */
        private fun collectContainerLogsAndStats(roots: List<FinishedTestResult>) {
            val logs = logAggregator.collectContainerLogs()
            val stats = statsAggregator.collectStats()
            for (test in roots) {
                matchLogsAndStatsToTest(test, logs, stats)
            }
        }

        /**
         * Matches logs and stats to the given test according to their timestamps. As the Test Runner's logs are already stored
         * in the results at this point, the container logs are appended and all entries are sorted by their timestamps.
         * @param test Test for which corresponding logs and traces are to be matched.
         * @param logs All logs captured from all Docker containers.
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

        /**
         * Returns the string representation of the source of the given test identifier.
         * Returns `null` in case this value is not set or not supported.
         */
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
         * @param result Aggregated result of the test execution.
         */
        private fun generateReports(result: TestExecutionResult) {
            val reportGeneratorClasses = Me2eTestConfigStorage.configAnnotation?.reportGenerators ?: arrayOf(HtmlReportGenerator::class)
            for (reportGeneratorClass in reportGeneratorClasses) {
                generateReport(reportGeneratorClass, result)
            }
        }

        /**
         * Generates the test report using the given [reportGeneratorClass].
         * @param reportGeneratorClass Class of the report generator to use.
         * @param result Aggregated result of the test execution.
         */
        private fun generateReport(reportGeneratorClass: KClass<out ReportGenerator>, result: TestExecutionResult) {
            if (reportGeneratorClass.primaryConstructor == null) {
                logger.error("Cannot use report generator $reportGeneratorClass, since it does not have a primary, no argument constructor.")
                return
            }
            try {
                val reportGenerator = reportGeneratorClass.primaryConstructor!!.callBy(emptyMap())
                logger.info("Generating test report using report generator $reportGeneratorClass...")
                reportGenerator.generate(result)
            } catch (e: Exception) {
                logger.error("Exception occurred while trying to generate report using $reportGeneratorClass.", e)
            }
        }

        /**
         * Collects all report entries published so far and resets the list of [collectedReportEntries].
         * This enables the report entries to be clearly assigned to the tests in which they were published.
         * @return List of report entries collected during the current test execution.
         */
        private fun collectReportEntries(): List<ReportEntry> {
            val entries = collectedReportEntries.toList()
            collectedReportEntries.clear()
            return entries
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
