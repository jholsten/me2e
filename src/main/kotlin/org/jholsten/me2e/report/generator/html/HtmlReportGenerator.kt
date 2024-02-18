package org.jholsten.me2e.report.generator.html

import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.report.generator.ReportGenerator
import org.jholsten.me2e.report.generator.html.data.IndexTemplateData
import org.jholsten.me2e.report.generator.html.data.TemplateData
import org.jholsten.me2e.report.generator.html.data.TestDetailTemplateData
import org.jholsten.me2e.report.result.model.TestExecutionResult
import org.jholsten.me2e.report.result.model.TestResult
import org.jholsten.me2e.utils.logger
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * Report generator which generates an HTML test report.
 * Includes an overview of all test results in an `index.html` file and detailed results for each test
 * class, each in a separate HTML file. To generate the HTML files, [Thymeleaf](https://www.thymeleaf.org/)
 * is used, which generates files from a template using a [org.thymeleaf.context.Context] instance, which
 * is filled with variables containing the test results.
 *
 * To extend or modify this report generator, implement a subclass and set its reference in your project's
 * [org.jholsten.me2e.Me2eTestConfig.reportGenerators].
 * @see <a href="https://www.thymeleaf.org/">Thymeleaf</a>
 * @constructor Instantiates a new HTML report generator.
 * @param indexTemplate Path to the Thymeleaf template to use for the `index.html`.
 * @param testDetailTemplate Path to the Thymeleaf template to use for the details of the execution of one test class.
 * @param additionalResources Additional resources required for the templates (e.g. `.css` or `.js` files).
 */
open class HtmlReportGenerator(
    /**
     * Path to the Thymeleaf template to use for the `index.html`.
     * Needs to be located in `resources` folder.
     */
    protected val indexTemplate: String = "report/templates/index.html",

    /**
     * Path to the Thymeleaf template to use for the details of the execution of one test class.
     * Needs to be located in `resources` folder.
     */
    protected val testDetailTemplate: String = "report/templates/test-detail.html",

    /**
     * Additional resources required for the templates (e.g. `.css` or `.js` files) to be
     * copied to the [outputDirectory] as map of destination path and source path.
     * All source files need to be located in `resources` folder.
     */
    protected val additionalResources: MutableMap<String, String> = mutableMapOf(
        "css/report-style.css" to "report/css/report-style.css",
        "tree-table/jquery.treetable.js" to "report/tree-table/jquery.treetable.js",
        "tree-table/jquery.treetable.css" to "report/tree-table/jquery.treetable.css",
    ),

    /**
     * Base directory where the generated HTML files are to be stored.
     */
    protected val outputDirectory: String = "build/reports/me2e"
) : ReportGenerator() {
    private val logger = logger<HtmlReportGenerator>()

    /**
     * Result of the test execution for which report is to be generated.
     * Is set after the test execution has finished when [generate] is invoked.
     */
    protected lateinit var result: TestExecutionResult

    /**
     * Timestamp of when this report was generated.
     */
    protected lateinit var generationTimestamp: Instant

    /**
     * URL to the HTML file which contains the overview of the test results.
     * Is logged after the report has been generated so that the user can open the report directly.
     */
    protected open val outputLink: String = "file:///${getAbsolutePath("$outputDirectory/index.html")}"

    override fun generate(result: TestExecutionResult) {
        generationTimestamp = Instant.now()
        this.result = result
        copyAdditionalResources()
        generateIndexHtml()
        for (root in result.roots) {
            generateTestDetailHtml(root)
        }
        logger.info("Generated and stored HTML report at $outputLink.")
    }

    /**
     * Generates HTML file containing an overview of the test execution's result.
     * Should create a file named `index.html` in the [outputDirectory].
     */
    protected open fun generateIndexHtml() {
        val data = IndexTemplateData.Builder()
            .withGenerationTimestamp(generationTimestamp)
            .withTestExecutionResult(result)
            .build()
        generateHtml(indexTemplate, data, "$outputDirectory/index.html")
    }

    /**
     * Generates HTML file containing the details of the given test result.
     * The result represents one test class, which may contain multiple tests and nested test classes.
     * Should create a file named [TestResult.source]`.html` in directory `sources` in the [outputDirectory].
     */
    protected open fun generateTestDetailHtml(result: TestResult) {
        val data = TestDetailTemplateData.Builder()
            .withGenerationTimestamp(generationTimestamp)
            .withTestResult(result)
            .build()
        generateHtml(testDetailTemplate, data, "$outputDirectory/sources/${result.source}.html")
    }

    /**
     * Generates HTML file using the given [template], filled with the given [data] and stores the
     * file in the given [outputPath].
     * @param template Path to the Thymeleaf template to be filled. Needs to be located in `resources` folder.
     * @param data Data containing the test results to be inserted into the Thymeleaf template.
     * @param outputPath Path where generated HTML file should be stored.
     */
    protected open fun generateHtml(template: String, data: TemplateData, outputPath: String) {
        TemplateEngine(template, data, outputPath).process()
    }

    /**
     * Copies additional resources required for the test report in the [outputDirectory].
     */
    protected open fun copyAdditionalResources() {
        for ((destination, source) in additionalResources) {
            val resource = FileUtils.getResourceAsStream(source)
            val destinationFile = File("$outputDirectory/$destination")
            destinationFile.mkdirs()
            Files.copy(resource, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Returns absolute path to the given [relativePath], relative to this classpath.
     * Replaces backslashes with forward slashes to generate OS independent URL.
     */
    private fun getAbsolutePath(relativePath: String): String {
        return FileSystems.getDefault().getPath(relativePath).toAbsolutePath().toString().replace("\\", "/")
    }
}
