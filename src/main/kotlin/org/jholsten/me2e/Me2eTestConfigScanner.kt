package org.jholsten.me2e

import io.github.classgraph.ClassGraph
import org.jholsten.me2e.utils.logger

/**
 * Utility class which scans the current project for instantiations of the [Me2eTestConfig] annotations.
 */
internal class Me2eTestConfigScanner {
    companion object {
        private val logger = logger<Me2eTestConfigScanner>()

        /**
         * Finds [Me2eTestConfig] annotation definition in the project. If multiple annotations are defined, the first one is used.
         * If no annotation is found, the default is returned.
         * @return First [Me2eTestConfig] annotation defined in the current project or `null`, if
         * no such instance could be found.
         */
        @JvmSynthetic
        fun findFirstTestConfigAnnotation(): Me2eTestConfig {
            ClassGraph().enableAnnotationInfo().disableJarScanning().scan().use { scanResult ->
                val annotatedClasses = scanResult.getClassesWithAnnotation(Me2eTestConfig::class.java)
                if (annotatedClasses.isEmpty()) {
                    logger.warn("Unable to find Me2eTestConfig annotation. Will be using the default values.")
                    return Me2eTestConfig()
                } else if (annotatedClasses.size != 1) {
                    logger.warn("Found ${annotatedClasses.size} Me2eTestConfig annotations. Will be using the first one...")
                }
                val annotatedClass = annotatedClasses.first()
                logger.info("Reading test configuration from ${annotatedClass.name}.")
                return annotatedClass.loadClass().getAnnotation(Me2eTestConfig::class.java)
            }
        }
    }
}
