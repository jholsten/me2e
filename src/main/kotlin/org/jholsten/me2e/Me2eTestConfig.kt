package org.jholsten.me2e

import org.jholsten.me2e.config.model.ConfigFormat

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
    val format: ConfigFormat,
    //val reportGenerator: Class<out ReportGenerator>, TODO
)
