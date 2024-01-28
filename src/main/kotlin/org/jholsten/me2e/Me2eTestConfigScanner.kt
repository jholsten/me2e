package org.jholsten.me2e

import io.github.classgraph.AnnotationEnumValue
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.utils.logger

internal class Me2eTestConfigScanner {
    companion object {
        private val logger = logger(this)

        /**
         * Finds [Me2eTestConfig] annotation definition in the project.
         * If multiple annotations are defined, the first one is used.
         */
        internal fun findFirstTestConfigAnnotation(): Me2eTestConfig? {
            val scanResult = ClassGraph().enableAnnotationInfo().scan()
            val annotatedClasses = scanResult.getClassesWithAnnotation(Me2eTestConfig::class.java)
            if (annotatedClasses.isEmpty()) {
                return null
            } else if (annotatedClasses.size != 1) {
                logger.warn("Found ${annotatedClasses.size} Me2eTestConfig annotations. Will be using the first one...")
            }
            val annotatedClass = annotatedClasses.first()
            logger.info("Reading test configuration from ${annotatedClass.name}")
            return extractAnnotationFromAnnotatedClass(annotatedClass)
        }

        private fun extractAnnotationFromAnnotatedClass(annotatedClass: ClassInfo): Me2eTestConfig {
            val annotation = annotatedClass.getAnnotationInfo(Me2eTestConfig::class.java)
            val params = annotation.getParameterValues(true)
            val config = params.find { it.name == "config" }!!.value as String
            val format = params.find { it.name == "format" }!!.value as AnnotationEnumValue
            return Me2eTestConfig(config = config, format = ConfigFormat.valueOf(format.valueName))
        }
    }
}
