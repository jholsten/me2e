package org.jholsten.me2e

import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.report.result.ReportGenerator
import org.jholsten.me2e.report.result.html.HtmlReportGenerator
import kotlin.reflect.KClass

/**
 * Configuration for the execution of the ME2E-Tests.
 *
 * This annotation can be placed anywhere in your project on any class.
 * When initializing the first class that inherits from [Me2eTest], the first annotation
 * in the project is searched for and used for the configuration of the test execution.
 * @see Me2eTest.configAnnotation
 * @see Me2eTestConfigScanner
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

    /**
     * Whether to assert that all containers are healthy before each test.
     * If activated, it is checked whether all containers for which a healthcheck is defined are currently healthy
     * before each test. Restarts containers if necessary and aborts tests if containers remain unhealthy.
     * @see Me2eAssertHealthyExtension
     */
    val assertHealthy: Boolean = true,

    /**
     * Configuration for resetting the state of containers, Mock Servers and databases after each test.
     * @see Me2eStateResetExtension
     */
    val stateResetConfig: StateReset = StateReset()
) {
    annotation class StateReset(
        /**
         * Whether to clear all entries from all tables for all database containers for which a connection to the
         * database is established via a [org.jholsten.me2e.container.database.connection.DatabaseConnection] after
         * each test.
         * @see Me2eStateResetExtension.clearDatabases
         */
        val clearAllTables: Boolean = true,

        /**
         * Whether to reset all request interceptors of all microservice containers after each test.
         * @see Me2eStateResetExtension.resetRequestInterceptors
         */
        val resetRequestInterceptors: Boolean = true,

        /**
         * Whether to reset all captured requests for all Mock Servers after each test.
         * @see Me2eStateResetExtension.resetMockServerRequests
         */
        val resetMockServerRequests: Boolean = true,
    )
}
