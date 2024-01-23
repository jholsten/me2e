package org.jholsten.me2e.report.result.html.data

import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import org.jholsten.me2e.report.result.model.*
import org.jholsten.me2e.report.result.utils.getDescendants
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
import org.thymeleaf.context.Context

/**
 * Data of a [TestResult] instance to be inserted into a Thymeleaf template.
 * Sets variables for the properties of the instance to be used in the [HtmlReportGenerator.testDetailTemplate].
 */
class TestDetailTemplateData(context: Context) : TemplateData(context) {
    class Builder : TemplateData.Builder<Builder>() {
        /**
         * Sets variables for the data contained in the given [result].
         * The following variables are available in the template:
         * - `testId:` [String] - Unique identifier of the test or test container (see [TestResult.testId]).
         * - `source:` [String] - Source of the [TestExecutionResult.roots] where this test or test container is defined (see [TestResult.source]).
         * - `path:` [List]<[Pair]<[String],[String]>> - Path of this result in the overall test execution tree from the root to this test (see [TestResult.path]).
         * - `children:` [List]<[TestResult]> - Summaries of the children of the test or test container (see [TestResult.children]).
         * - `allTests:` [List]<[TestResult]> - All tests and test containers included in the result, i.e. all of the [TestResult.children],
         * their children and their children, recursively.
         * - `url:` [String] - URL of the HTML file of the result.
         * - `status:` [TestResult.Status] - Status of the test execution (see [TestResult.status]).
         * - `numberOfTests:` [Int] - Number of tests that the result contains (see [TestResult.numberOfTests]).
         * - `numberOfFailures:` [Int] - Number of failed tests that the result contains (see [TestResult.numberOfFailures]).
         * - `numberOfSkipped`: [Int] - Number of skipped tests that the result contains (see [TestResult.numberOfSkipped]).
         * - `successRate`: [Int]? - Relative share of successful tests in the number of tests that the result contains (see [TestResult.successRate]).
         * - `displayName:` [String] - Human-readable name of the test or test container (see [TestResult.displayName]).
         * - `tags:` [Set]<[String]> - Tags associated with the test or test container (see [TestResult.tags]).
         * - `startTime:` [java.time.Instant] - Timestamp of the test or test container has started its execution
         * (see [FinishedTestResult.startTime]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `endTime:` [java.time.Instant] - Timestamp of the test or test container has finished its execution
         * (see [FinishedTestResult.endTime]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `duration:` [Long] - Number of seconds that the test execution took (see [FinishedTestResult.duration]).
         * **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `reportEntries:` [List]<[ReportEntry]> - Additional report entries that were published during the test execution
         * (see [FinishedTestResult.reportEntries]). **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `logs:` [AggregatedLogEntryList] - Logs that were collected for the test execution (see [FinishedTestResult.logs]).
         * **Only available if [TestResult.status] is not [TestResult.Status.SKIPPED]**.
         * - `stats:` [AggregatedStatsEntryList] - Resource usage statistics that were collected for the test execution
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
            withVariable("allTests", getAllTests(result))
            withVariable("url", "${result.source}.html")
            withVariable("status", result.status)
            withVariable("numberOfTests", result.numberOfTests)
            withVariable("numberOfFailures", result.numberOfFailures)
            withVariable("successRate", result.successRate)
            withVariable("displayName", result.displayName)
            withVariable("tags", result.tags)
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

        override fun self(): Builder {
            return this
        }

        private fun getAllTests(result: TestResult): List<TestResult> {
            val tests: MutableList<TestResult> = mutableListOf()
            for (child in result.children) {
                tests.add(child)
                tests.addAll(getDescendants(child))
            }
            return tests
        }
    }
}
