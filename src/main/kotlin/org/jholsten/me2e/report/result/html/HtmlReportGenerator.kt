package org.jholsten.me2e.report.result.html

import org.jholsten.me2e.report.result.ReportGenerator
import org.jholsten.me2e.report.result.model.TestExecutionResult

/**
 * Report generator which generates an HTML test report.
 */
open class HtmlReportGenerator(
    /**
     * Path to the Thymeleaf template to use for the `index.html`.
     * Needs to be located in resources folder.
     */
    protected val indexTemplate: String = "report/templates/index.html",

    /**
     * Path to the Thymeleaf template to use for the details of the execution of one test.
     * Needs to be located in resources folder.
     */
    protected val testDetailTemplate: String = "report/templates/test-detail.html",

    /**
     * Base directory where the generated HTML files are to be stored.
     */
    protected val outputDirectory: String = "build/reports/me2e"
) : ReportGenerator() {
    protected lateinit var summaries: List<TestSummary>

    override fun generate(summaries: List<TestSummary>) {
        this.summaries = summaries
    }

    open fun generateIndexHtml() {
        // TODO
    }

    open fun generateTestDetailHtml() {

    }
}
