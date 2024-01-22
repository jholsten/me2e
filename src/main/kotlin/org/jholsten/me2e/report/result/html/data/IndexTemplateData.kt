package org.jholsten.me2e.report.result.html.data

import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.thymeleaf.context.Context

/**
 * Data of a [TestExecutionResult] instance to be inserted into a Thymeleaf template.
 * Sets variables for the properties of the instance to be used in the [HtmlReportGenerator.indexTemplate].
 */
class IndexTemplateData(context: Context) : TemplateData(context) {
    class Builder : TemplateData.Builder<Builder>() {
        /**
         * Sets variables for the data contained in the given [result].
         * The following variables are available in the template:
         * - `numberOfTests:` [Int] - Total number of tests that were executed (see [TestExecutionResult.numberOfTests]).
         * - `numberOfFailures:` [Int] - Total number of failed tests (see [TestExecutionResult.numberOfFailures]).
         * - `numberOfSkipped:` [Int] - Total number of tests which were skipped (see [TestExecutionResult.numberOfSkipped]).
         * - `successRate:` [Int]? - Relative share of successful tests in the number of tests that the result contains (see [TestExecutionResult.successRate]).
         * - `duration:` [java.math.BigDecimal]? - Number of seconds that executing all tests took (see [TestExecutionResult.duration]).
         * - `roots:` [List]<[TestResult]> - Roots of all tests that have been performed (see [TestExecutionResult.roots]).
         * - `tests:` [List]<[TestResult]> - All tests included in the result (see [TestExecutionResult.tests]).
         */
        fun withTestExecutionResult(result: TestExecutionResult) = apply {
            withVariable("numberOfTests", result.numberOfTests)
            withVariable("numberOfFailures", result.numberOfFailures)
            withVariable("numberOfSkipped", result.numberOfSkipped)
            withVariable("successRate", result.successRate)
            withVariable("duration", result.duration)
            withVariable("roots", result.roots)
            withVariable("tests", result.tests)
        }

        override fun build(): IndexTemplateData {
            return IndexTemplateData(super.context)
        }

        override fun self(): Builder {
            return this
        }
    }
}
