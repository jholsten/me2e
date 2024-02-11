package org.jholsten.me2e.report.generator

import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.report.result.model.TestExecutionResult

/**
 * Base class for generating the test report based on the data provided in the [ReportDataAggregator].
 * The method [generate] is called as soon as the execution of all tests has finished.
 * @sample org.jholsten.samples.report.CustomHtmlReportGenerator
 * @sample org.jholsten.samples.report.LoggingReportGenerator
 * @constructor Instantiates a new report generator.
 */
abstract class ReportGenerator {
    /**
     * Generates a test report for the test execution result provided in [result].
     * @param result Result of the execution of all test containers and all tests.
     */
    abstract fun generate(result: TestExecutionResult)
}
