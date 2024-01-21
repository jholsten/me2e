package org.jholsten.me2e.report.result

import org.jholsten.me2e.report.result.model.TestExecutionResult

/**
 * Base class for generating the test report based on the data provided in the [ReportDataAggregator].
 * The method [generate] is called as soon as the execution of all tests has finished.
 * @sample org.jholsten.me2e.report.summary.html.HtmlReportGenerator
 */
abstract class ReportGenerator {
    /**
     * Generates a test report for the test execution summaries provided in [summaries].
     * @param summaries List of summaries for each test and test container execution.
     */
    abstract fun generate(summaries: List<TestSummary>)
}
