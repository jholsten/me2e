package org.jholsten.me2e.report.result.html.data

import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.report.result.utils.getDescendants
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
         * - `numberOfAborted:` [Int] - Total number of tests which were aborted (see [TestExecutionResult.numberOfAborted]).
         * - `successRate:` [Int]? - Relative share of successful tests in the number of tests that the result contains (see [TestExecutionResult.successRate]).
         * - `duration:` [java.math.BigDecimal]? - Number of seconds that executing all tests took (see [TestExecutionResult.duration]).
         * - `roots:` [List]<[TestResult]> - Roots of all tests that have been performed (see [TestExecutionResult.roots]).
         * - `allTests:` [List]<[TestResult]> - All tests and test containers included in the result, i.e. all of the [TestExecutionResult.roots],
         * their children and their children, recursively.
         */
        fun withTestExecutionResult(result: TestExecutionResult) = apply {
            withVariable("numberOfTests", result.numberOfTests)
            withVariable("numberOfFailures", result.numberOfFailures)
            withVariable("numberOfSkipped", result.numberOfSkipped)
            withVariable("numberOfAborted", result.numberOfAborted)
            withVariable("successRate", result.successRate)
            withVariable("duration", result.duration)
            withVariable("roots", result.roots)
            withVariable("allTests", getAllTests(result))
        }

        override fun build(): IndexTemplateData {
            return IndexTemplateData(super.context)
        }

        override fun self(): Builder {
            return this
        }

        private fun getAllTests(result: TestExecutionResult): List<TestResult> {
            val tests: MutableList<TestResult> = mutableListOf()
            for (root in result.roots) {
                tests.add(root)
                tests.addAll(getDescendants(root))
            }
            return tests
        }
    }
}
