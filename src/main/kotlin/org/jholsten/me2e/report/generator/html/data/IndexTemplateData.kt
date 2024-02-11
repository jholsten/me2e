package org.jholsten.me2e.report.generator.html.data

import org.jholsten.me2e.report.generator.html.HtmlReportGenerator
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.report.result.utils.getDescendants
import org.thymeleaf.context.Context

/**
 * Data of a [TestExecutionResult] instance to be inserted into a Thymeleaf template.
 * Sets variables for the properties of the instance to be used in the [HtmlReportGenerator.indexTemplate].
 * @constructor Instantiates a new instance for specifying the data of the index template. To automatically
 * set the variables for the test results, use the [Builder] instead.
 * @param context Thymeleaf context containing the data to set.
 */
class IndexTemplateData(context: Context) : TemplateData(context) {
    /**
     * Builder for instantiating instances of [IndexTemplateData].
     * @constructor Instantiates a new builder instance for constructing [IndexTemplateData].
     */
    class Builder : TemplateData.Builder<Builder>() {
        /**
         * Sets variables for the data contained in the given [result].
         * Subsequently, following variables are available in the template:
         * - `numberOfTests:` [Int] - Total number of tests that were executed (see [TestExecutionResult.numberOfTests]).
         * - `numberOfFailures:` [Int] - Total number of failed tests (see [TestExecutionResult.numberOfFailures]).
         * - `numberOfSkipped:` [Int] - Total number of tests which were skipped (see [TestExecutionResult.numberOfSkipped]).
         * - `numberOfAborted:` [Int] - Total number of tests which were aborted (see [TestExecutionResult.numberOfAborted]).
         * - `successRate:` [Int]? - Relative share of successful tests in the number of tests that the result contains (see [TestExecutionResult.successRate]).
         * - `duration:` [java.math.BigDecimal]? - Number of seconds that executing all tests took (see [TestExecutionResult.duration]).
         * - `roots:` [List]<[TestResult]> - Roots of all tests included in the result (see [TestExecutionResult.roots]).
         * - `allTests:` [List]<[TestResult]> - All tests and test containers included in the result, i.e. all of the [TestExecutionResult.roots],
         * their children and their children, recursively.
         * @param result Result of the execution of all tests.
         * @return This builder instance, to use for chaining.
         */
        fun withTestExecutionResult(result: TestExecutionResult): Builder = apply {
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

        /**
         * Returns all tests and test containers included in the result, i.e. all of the [TestExecutionResult.roots],
         * their children and their children, recursively.
         * @param result Test execution result for which all tests should be retrieved.
         */
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
