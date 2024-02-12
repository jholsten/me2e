package org.jholsten.me2e

import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.report.generator.ReportGenerator
import org.jholsten.me2e.report.generator.html.HtmlReportGenerator
import kotlin.reflect.KClass

/**
 * Configuration for the execution of the ME2E-Tests.
 *
 * This annotation can be placed anywhere in your project on any class.
 * When initializing the first class that inherits from [Me2eTest], the first annotation
 * in the project is searched for and used for the configuration of the test execution.
 * @see Me2eTest.configAnnotation
 * @see Me2eTestConfigScanner
 * @constructor Instantiates a new configuration for the ME2E tests.
 * @param config Path to the configuration file.
 * @param format Format of the configuration file.
 * @param reportGenerators Report generators to use for generating the test report.
 */
@Target(AnnotationTarget.CLASS)
annotation class Me2eTestConfig(
    /**
     * Path to the configuration file. Needs to be located in `resources` folder.
     * @see org.jholsten.me2e.config.model.TestConfig
     */
    val config: String,

    /**
     * Format of the configuration file.
     */
    val format: ConfigFormat = ConfigFormat.YAML,

    /**
     * Report generators to use for generating the test report.
     * Each of these report generators is invoked one after the other at the end of the test execution.
     * @see ReportGenerator
     * @see org.jholsten.me2e.report.result.ReportDataAggregator.generateReports
     */
    val reportGenerators: Array<KClass<out ReportGenerator>> = [HtmlReportGenerator::class],
)
