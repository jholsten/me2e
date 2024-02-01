package org.jholsten.me2e

import org.jholsten.me2e.container.Container
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 extension which - if activated in the [Me2eTestConfig] - checks before each test whether all Docker
 * containers for which a health check is defined are currently healthy. If one of the containers is not healthy,
 * it is assumed that this would lead to a flaky and therefore not meaningful test result. Therefore, an attempt
 * is made to restart the container once. If this attempt fails or if the container is still unhealthy, the test
 * is skipped.
 */
class Me2eAssertHealthyExtension : BeforeEachCallback {
    companion object {
        /**
         * List of Docker containers that have already been restarted once. If one of the containers in this
         * list is unhealthy a second time, no new restart attempt is made and test is skipped.
         */
        private val restartedContainers: MutableList<Container> = mutableListOf()
    }

    override fun beforeEach(context: ExtensionContext?) {

        TODO("Not yet implemented")
    }
}
