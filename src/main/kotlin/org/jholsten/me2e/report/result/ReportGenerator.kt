package org.jholsten.me2e.report.result

import org.jholsten.me2e.report.result.model.TestExecutionResult

/**
 * Base class for generating the test report based on the data provided in the [ReportDataAggregator].
 * The method [generate] is called as soon as the execution of all tests has finished.
 * @sample org.jholsten.me2e.report.result.html.HtmlReportGenerator
 */
abstract class ReportGenerator {
    /**
     * Generates a test report for the test execution result provided in [result].
     * @param result Result of the execution of all test containers and all tests.
     */
    abstract fun generate(result: TestExecutionResult)
}
