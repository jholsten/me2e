package org.jholsten.me2e

import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.report.result.ReportGenerator
import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import kotlin.reflect.KClass

/**
 * Annotation for the configuration of the ME2E-Tests.
 * This method can be placed anywhere in the project and the first annotation
 * that is found is used to configure the library and start the environment.
 */
@Target(AnnotationTarget.CLASS)
annotation class Me2eTestConfig(
    /**
     * Path to the configuration. Needs to be located in `resources` folder.
     */
    val config: String,

    /**
     * Format of the configuration file.
     */
    val format: ConfigFormat = ConfigFormat.YAML,

    /**
     * Report generators to use for generating the test report.
     */
    val reportGenerators: Array<KClass<out ReportGenerator>> = [HtmlReportGenerator::class],

    /**
     * Whether to assert that all containers are healthy before each test.
     */
    val assertHealthy: Boolean = true,
)
