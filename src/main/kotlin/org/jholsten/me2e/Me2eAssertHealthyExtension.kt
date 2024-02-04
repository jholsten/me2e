package org.jholsten.me2e

import org.jholsten.me2e.container.health.exception.HealthTimeoutException
import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.opentest4j.TestAbortedException

/**
 * JUnit5 extension which - if activated in the [Me2eTestConfig] - checks before each test whether all Docker
 * containers for which a healthcheck is defined are currently healthy. If one of the containers is not healthy,
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

    /**
     * Callback function which is executed before the execution of a test starts.
     * If activated in the [Me2eTestConfig], it checks whether all Docker containers for which a healthcheck is defined,
     * are currently healthy. If one of the containers is not healthy, an attempt is made to restart the container once.
     * If this attempt fails or if the container is still unhealthy, the test is aborted.
     */
    override fun beforeEach(context: ExtensionContext?) {
        if (Me2eTest.configAnnotation.assertHealthy) {
            checkContainerHealth()
        }
    }

    /**
     * Checks whether any of the containers in the Docker-Compose environment is currently unhealthy.
     * If there are any containers for which a healthcheck is defined and which are currently unhealthy, they are restarted.
     */
    private fun checkContainerHealth() {
        val unhealthyContainers = Me2eTest.containerManager.containers.values.filter { it.hasHealthcheck && !it.isHealthy }.map { it.name }
        if (unhealthyContainers.isEmpty()) {
            return
        }
        restartContainers(unhealthyContainers)
    }

    /**
     * Restarts the given list of unhealthy container names. If any of these containers were already restarted previously,
     * the current test is aborted by throwing a [TestAbortedException].
     */
    private fun restartContainers(unhealthyContainers: List<String>) {
        logger.warn("Found ${unhealthyContainers.size} containers to be unhealthy: [${unhealthyContainers.joinToString(", ")}].")
        val containersToRestart = unhealthyContainers.filter { it !in restartedContainers }
        if (unhealthyContainers.size != containersToRestart.size) {
            val deadContainers = unhealthyContainers - containersToRestart.toSet()
            val message = "${deadContainers.size} containers were already restarted before: [${deadContainers.joinToString(", ")}]. " +
                "Since they are unhealthy again - even after a restart - the test is aborted. " +
                "To disable this behavior, set 'assertHealthy' in the 'Me2eTestConfig' to false."
            logger.warn(message)
            throw TestAbortedException(message)
        }
        restartedContainers.addAll(unhealthyContainers)
        try {
            Me2eTest.containerManager.restart(containersToRestart)
        } catch (e: HealthTimeoutException) {
            throw TestAbortedException(
                "Test was aborted since at least one of the containers in the environment did not become healthy after a restart.",
                e
            )
        }
    }
}
