package org.jholsten.me2e

import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.opentest4j.TestAbortedException

/**
 * JUnit5 extension which - if activated in the [Me2eTestConfig] - checks before each test whether all Docker
 * containers for which a health check is defined are currently healthy. If one of the containers is not healthy,
 * it is assumed that this would lead to a flaky and therefore not meaningful test result. Therefore, an attempt
 * is made to restart the container once. If this attempt fails or if the container is still unhealthy, the test
 * is aborted.
 */
class Me2eAssertHealthyExtension : BeforeEachCallback {
    companion object {
        /**
         * List of Docker container names that have already been restarted once. If one of the containers in this
         * list is unhealthy a second time, no new restart attempt is made and the test is aborted.
         */
        private val restartedContainers: MutableList<String> = mutableListOf()
    }

    private val logger = logger(this)

    override fun beforeEach(context: ExtensionContext?) {
        if (Me2eTest.configAnnotation.assertHealthy) {
            checkContainerHealth()
        }
    }

    private fun checkContainerHealth() {
        val unhealthyContainers = Me2eTest.containerManager.containers.values.filter { it.hasHealthcheck && !it.isHealthy }.map { it.name }
        if (unhealthyContainers.isEmpty()) {
            return
        }
        logger.warn("Found ${unhealthyContainers.size} containers to be unhealthy.")
        val containersToRestart = unhealthyContainers.filter { it !in restartedContainers }
        if (unhealthyContainers.size != containersToRestart.size) {
            val deadContainers = unhealthyContainers - containersToRestart.toSet()
            logger.warn("${deadContainers.size} containers were already restarted before: [${deadContainers.joinToString(", ")}]. Since they are still unhealthy, the test is aborted.")
            throw TestAbortedException("Test was aborted since at least one of the containers in the environment is not healthy even after a restart.")
        }
        restartedContainers.addAll(unhealthyContainers)
        Me2eTest.containerManager.restart(containersToRestart)
    }
}
