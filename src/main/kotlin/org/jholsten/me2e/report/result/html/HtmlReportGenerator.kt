package org.jholsten.me2e.report.result.html

import org.jholsten.me2e.report.result.ReportGenerator
import org.jholsten.me2e.report.result.html.data.IndexTemplateData
import org.jholsten.me2e.report.result.html.data.TemplateData
import org.jholsten.me2e.report.result.html.data.TestDetailTemplateData
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.slf4j.LoggerFactory

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
    private val logger = LoggerFactory.getLogger(HtmlReportGenerator::class.java)
    protected lateinit var result: TestExecutionResult

    override fun generate(result: TestExecutionResult) {
        this.result = result
        generateIndexHtml()
        val junitResult = result.tests.find { it.testId == "[engine:junit-jupiter]" }
        if (junitResult == null) {
            logger.warn("Unable to find test results of JUnit engine. Cannot generate detailed test reports.")
        } else {
            generateTestDetailHtml(junitResult)
        }
    }

    protected open fun generateIndexHtml() {
        val data = IndexTemplateData.Builder().withTestExecutionResult(result).build()
        generateHtml(indexTemplate, data, "$outputDirectory/index.html")
    }

    protected open fun generateTestDetailHtml(result: TestResult) {
        val data = TestDetailTemplateData.Builder().withTestResult(result).build()
        generateHtml(testDetailTemplate, data, "$outputDirectory/detail/TODO.html")
        result.children.forEach { generateTestDetailHtml(it) }
    }

    protected open fun generateHtml(template: String, data: TemplateData, outputPath: String) {
        TemplateEngine(template, data, outputPath).process()
    }
}
