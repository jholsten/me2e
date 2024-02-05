package org.jholsten.me2e.report.result.html

import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.report.result.ReportGenerator
import org.jholsten.me2e.report.result.html.data.IndexTemplateData
import org.jholsten.me2e.report.result.html.data.TemplateData
import org.jholsten.me2e.report.result.html.data.TestDetailTemplateData
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

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
     * Additional resources required for the templates (e.g. `.css` or `.js` files) to be
     * copied to the [outputDirectory] as map of source path and destination path.
     * All source files need to be located in resources folder.
     */
    protected val additionalResources: Map<String, String> = mapOf(
        "report/css/report-style.css" to "css/report-style.css",
        "report/tree-table/jquery.treetable.js" to "tree-table/jquery.treetable.js",
        "report/tree-table/jquery.treetable.css" to "tree-table/jquery.treetable.css",
    ),

    /**
     * Base directory where the generated HTML files are to be stored.
     */
    protected val outputDirectory: String = "build/reports/me2e"
) : ReportGenerator() {
    private val logger = LoggerFactory.getLogger(HtmlReportGenerator::class.java)
    protected lateinit var result: TestExecutionResult
    private lateinit var generationTimestamp: Instant

    override fun generate(result: TestExecutionResult) {
        generationTimestamp = Instant.now()
        this.result = result
        copyAdditionalResources()
        generateIndexHtml()
        for (root in result.roots) {
            generateTestDetailHtml(root)
        }
        val absolutePath = FileSystems.getDefault().getPath("$outputDirectory/index.html").toAbsolutePath().toString().replace("\\", "/")
        logger.info("Generated and stored HTML report at file:///$absolutePath.")
    }

    protected open fun generateIndexHtml() {
        val data = IndexTemplateData.Builder()
            .withGenerationTimestamp(generationTimestamp)
            .withTestExecutionResult(result)
            .build()
        generateHtml(indexTemplate, data, "$outputDirectory/index.html")
    }

    protected open fun generateTestDetailHtml(result: TestResult) {
        val data = TestDetailTemplateData.Builder()
            .withGenerationTimestamp(generationTimestamp)
            .withTestResult(result)
            .build()
        generateHtml(testDetailTemplate, data, "$outputDirectory/sources/${result.source}.html")
    }

    protected open fun generateHtml(template: String, data: TemplateData, outputPath: String) {
        TemplateEngine(template, data, outputPath).process()
    }

    protected open fun copyAdditionalResources() {
        for ((source, destination) in additionalResources) {
            val resource = FileUtils.getResourceAsStream(source)
            Files.copy(resource, File("$outputDirectory/$destination").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
