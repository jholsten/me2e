package org.jholsten.me2e.report.generator.html.data

import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.generator.html.HtmlReportGenerator
import org.jholsten.me2e.report.result.model.*
import org.jholsten.me2e.report.result.utils.getDescendants
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry
import org.thymeleaf.context.Context
import java.time.Instant

/**
 * Data of a [TestResult] instance to be inserted into a Thymeleaf template.
 * Sets variables for the properties of the instance to be used in the [HtmlReportGenerator.testDetailTemplate].
 */
class TestDetailTemplateData(context: Context) : TemplateData(context) {
    class Builder : TemplateData.Builder<Builder>() {
        /**
         * Sets variables for the data contained in the given [result].
         * Subsequently, the following variables are available in the template:
         * - `testId:` [String] - Unique identifier of the test or test container (see [TestResult.testId]).
         * - `source:` [String] - Source of the [TestExecutionResult.roots] where this test or test container is defined (see [TestResult.source]).
         * - `path:` [List]<[Pair]<[String],[String]>> - Path of this result in the overall test execution tree from the root to this test (see [TestResult.path]).
         * - `children:` [List]<[TestResult]> - Results of the children of the test or test container (see [TestResult.children]).
         * - `allTests:` [List]<[TestResult]> - All tests and test containers included in the result, i.e. the [result], all of the
         * [TestResult.children], their children and their children, recursively.
         * - `status:` [TestResult.Status] - Status of the test execution (see [TestResult.status]).
         * - `numberOfTests:` [Int] - Number of tests that the result contains (see [TestResult.numberOfTests]).
         * - `numberOfFailures:` [Int] - Number of failed tests that the result contains (see [TestResult.numberOfFailures]).
         * - `numberOfSkipped`: [Int] - Number of skipped tests that the result contains (see [TestResult.numberOfSkipped]).
         * - `numberOfAborted`: [Int] - Number of aborted tests that the result contains (see [TestResult.numberOfAborted]).
         * - `successRate`: [Int]? - Relative share of successful tests in the number of tests that the result contains (see [TestResult.successRate]).
         * - `displayName:` [String] - Human-readable name of the test or test container (see [TestResult.displayName]).
         * - `tags:` [Set]<[String]> - Tags associated with the test or test container (see [TestResult.tags]).
         * - `statsByContainer:` [Map]<[String], [List]<[AggregatedStatsEntry]>> - Map of container name and their resource usage statistics.
         * - `loggingServices:` [Map]<[String], [List]<[ServiceSpecification]>> - Map of `testId` and a list of distinct and sorted services
         * which logged at least one entry for this test. Includes only tests for which logs were captured. Can be used to filter logs by service.
         * - `tracesTimeSeries:` [Map]<[String], [List]<[Instant]>> - Map of `testId` and the timestamps of which the time series of the
         * test's traces is composed of. Includes only tests for which traces were captured.
         * - `startTime:` [java.time.Instant] - Timestamp of when the test or test container has started its execution
         * (see [FinishedTestResult.startTime]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `endTime:` [java.time.Instant] - Timestamp of when the test or test container has finished its execution
         * (see [FinishedTestResult.endTime]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `duration:` [Long] - Number of seconds that the test execution took (see [FinishedTestResult.duration]).
         * **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `reportEntries:` [List]<[ReportEntry]> - Additional report entries that were published during the test execution
         * (see [FinishedTestResult.reportEntries]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `logs:` [List]<[AggregatedLogEntry]> - Logs that were collected for the test execution (see [FinishedTestResult.logs]).
         * **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `stats:` [List]<[AggregatedStatsEntry]> - Resource usage statistics that were collected for the test execution
         * (see [FinishedTestResult.stats]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `throwable:` [Throwable]? - Throwable that caused the test result (see [FinishedTestResult.throwable]).
         * **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `stackTrace:` [String]? - String representation of the stack trace of the throwable that caused this result
         * (see [FinishedTestResult.stackTrace]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `reason:` [String]? - Message describing why the execution has been skipped (see [SkippedTestResult.reason]).
         * **Only available if [TestResult.status] is [TestResult.Status.SKIPPED]**.
         */
        fun withTestResult(result: TestResult) = apply {
            withVariable("testId", result.testId)
            withVariable("source", result.source)
            withVariable("path", result.path)
            withVariable("children", result.children)
            withVariable("status", result.status)
            withVariable("numberOfTests", result.numberOfTests)
            withVariable("numberOfFailures", result.numberOfFailures)
            withVariable("numberOfSkipped", result.numberOfSkipped)
            withVariable("numberOfAborted", result.numberOfAborted)
            withVariable("successRate", result.successRate)
            withVariable("displayName", result.displayName)
            withVariable("tags", result.tags)
            val allTests = getAllTests(result)
            withVariable("allTests", allTests)
            withVariable("statsByContainer", getStatsByContainer(result))
            withVariable("loggingServices", getLoggingServices(allTests))
            withVariable("tracesTimeSeries", getTracesTimeSeries(allTests))
            if (result is FinishedTestResult) {
                withVariable("startTime", result.startTime)
                withVariable("endTime", result.endTime)
                withVariable("duration", result.duration)
                withVariable("reportEntries", result.reportEntries)
                withVariable("logs", result.logs)
                withVariable("stats", result.stats)
                withVariable("throwable", result.throwable)
                withVariable("stackTrace", result.stackTrace)
            } else if (result is SkippedTestResult) {
                withVariable("reason", result.reason)
            }
        }

        override fun build(): TestDetailTemplateData {
            return TestDetailTemplateData(super.context)
        }

        /**
         * Returns all tests and test containers included in the result, i.e. the [result], all of its children
         * and their children, recursively.
         * @param result Test result for which all tests should be retrieved.
         */
        private fun getAllTests(result: TestResult): List<TestResult> {
            val tests: MutableList<TestResult> = mutableListOf(result)
            for (child in result.children) {
                tests.add(child)
                tests.addAll(getDescendants(child))
            }
            return tests
        }

        /**
         * Returns map of container name and their resource usage statistics which were captured for the execution
         * of the [result] and its children.
         * @param result Test result for which container statistics grouped by container names should be retrieved.
         */
        private fun getStatsByContainer(result: TestResult): Map<String, List<AggregatedStatsEntry>> {
            return if (result is FinishedTestResult) {
                result.stats.groupBy { it.service.name }
            } else {
                mapOf()
            }
        }

        /**
         * Returns map of [TestResult.testId] and a sorted list of services which logged entries for this test.
         * Can be used to filter logs by services.
         * @param allTests All tests and test containers included in the result.
         * @return Map of test ID and the services which logged at least one entry for this test.
         */
        private fun getLoggingServices(allTests: List<TestResult>): Map<String, List<ServiceSpecification>> {
            val result: MutableMap<String, List<ServiceSpecification>> = mutableMapOf()
            val testsWithLogs = allTests.filterIsInstance<FinishedTestResult>().filter { it.logs.isNotEmpty() }
            for (test in testsWithLogs) {
                result[test.testId] = test.logs.map { it.service }.distinctBy { it.name }.sortedBy { it.name }
            }
            return result
        }

        /**
         * Returns map of [TestResult.testId] and the list of timestamps of which the time series of
         * the test's traces is composed of. Each timeslot is 100 milliseconds long.
         * Only includes tests for which traces were captured.
         * @param allTests All tests and test containers included in the result.
         * @return Map of test ID and the timeslots of which the trace's time series is composed of.
         */
        private fun getTracesTimeSeries(allTests: List<TestResult>): Map<String, List<Instant>> {
            val result: MutableMap<String, List<Instant>> = mutableMapOf()
            val testsWithTraces = allTests.filterIsInstance<FinishedTestResult>().filter { it.traces.isNotEmpty() }
            for (test in testsWithTraces) {
                val seriesStart = test.traces.minOf { it.request.timestamp }.floor()
                val seriesEnd = test.traces.maxOf { it.response.timestamp }.ceil()
                result[test.testId] = getTimeSeries(seriesStart, seriesEnd)
            }
            return result
        }

        /**
         * Rounds the given instant down to the nearest 100th millisecond.
         */
        private fun Instant.floor(): Instant {
            val milliseconds = this.toEpochMilli()
            return Instant.ofEpochMilli(milliseconds / 100 * 100)
        }

        /**
         * Rounds the given instant up to the nearest 100th millisecond.
         */
        private fun Instant.ceil(): Instant {
            return this.floor().plusMillis(100)
        }

        /**
         * Returns all timeslots starting from [seriesStart] up to [seriesEnd], each of which is 100 milliseconds long.
         * @param seriesStart First timestamp of the series.
         * @param seriesEnd Last timestamp of the series.
         * @return List of 100 milliseconds timeslots starting from [seriesStart] up to [seriesEnd].
         */
        private fun getTimeSeries(seriesStart: Instant, seriesEnd: Instant): List<Instant> {
            val result: MutableList<Instant> = mutableListOf()
            var current = seriesStart
            while (current.isBefore(seriesEnd)) {
                result.add(current)
                current = current.plusMillis(100)
            }
            result.add(seriesEnd)
            return result
        }
    }
}
