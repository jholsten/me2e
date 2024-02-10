package org.jholsten.samples.report

import org.jholsten.me2e.report.generator.ReportGenerator
import org.jholsten.me2e.report.generator.html.HtmlReportGenerator
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.slf4j.LoggerFactory

/**
 * Sample of a custom [HtmlReportGenerator] which uses a custom Thymeleaf template for the `index.html`.
 */
class CustomHtmlReportGenerator : HtmlReportGenerator(
    indexTemplate = "report/templates/custom-index.html",
)

/**
 * Sample of a custom [ReportGenerator] which simply logs the test result.
 */
class LoggingReportGenerator : ReportGenerator() {
    private val logger = LoggerFactory.getLogger(LoggingReportGenerator::class.java)

    override fun generate(result: TestExecutionResult) {
        logger.info("Test execution finished with result $result")
    }
}
