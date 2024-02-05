package org.jholsten.me2e

import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 extension which - if activated in the [Me2eTestConfig] - resets all request interceptors
 * of all microservice containers after each test. This ensures a more predictable behavior so that
 * request interceptors set in previous tests do not influence the result of other tests.
 */
class Me2eRequestInterceptorResetExtension : AfterEachCallback {
    private val logger = logger(this)

    override fun afterEach(context: ExtensionContext?) {
        if (Me2eTest.configAnnotation.resetRequestInterceptors) {
            resetRequestInterceptors()
        }
    }

    /**
     * Resets request interceptors of all microservice containers.
     */
    private fun resetRequestInterceptors() {
        val microservices = Me2eTest.containerManager.microservices.values
        if (microservices.isNotEmpty()) {
            logger.debug("Resetting ${microservices.size} request interceptors of microservice containers.")
            for (microservice in microservices) {
                microservice.resetRequestInterceptors()
            }
        }
    }
}
