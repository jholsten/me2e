package org.jholsten.me2e

import org.jholsten.me2e.config.model.StateResetConfig
import org.jholsten.me2e.config.model.TestSettings
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.health.exception.HealthTimeoutException
import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.opentest4j.TestAbortedException

/**
 * Junit5 extension for executing the Me2eTests.
 * Includes the following callbacks:
 * - `@BeforeAll`: Starts the test environment if it has not yet been started. Aborts the execution of test classes if starting
 * the environment has failed.
 * - `@BeforeEach`: Checks whether all Docker containers for which a healthcheck is defined are currently healthy, if not
 * deactivated in [TestSettings.assertHealthy].
 * - `@AfterEach`: Resets the state of all containers, Mock Servers and databases, if not deactivated in [TestSettings.stateReset].
 *
 * ### `@BeforeAll`-Callback
 * The `@BeforeAll`-Callback checks whether the test environment is up and running before the execution of each test class.
 * If starting the environment has failed, all test classes are aborted, as it is only possible to interact with the containers when
 * the environment is running.
 *
 * ### `@BeforeEach`-Callback
 * If activated in [TestSettings.assertHealthy], the `@BeforeEach`-Callback checks whether all Docker containers for which a
 * healthcheck is defined are currently healthy before each test. If one of the containers is not healthy, it is assumed that
 * this would lead to a flaky and therefore not meaningful test result. Therefore, an attempt is made to restart the container once.
 * If this attempt fails or if the container is still unhealthy, the test is aborted.
 *
 * ### `@AfterEach`-Callback
 * If activated in the [TestSettings.stateReset], the `@AfterEach`-Callback resets the state of all containers, Mock Servers
 * and databases after each test. Includes the following states:
 * - [org.jholsten.me2e.container.database.DatabaseContainer]: Database entries
 * - [org.jholsten.me2e.container.microservice.MicroserviceContainer]: Request Interceptors
 * - [org.jholsten.me2e.mock.MockServer]: Received requests
 *
 * Resetting the state after each test ensures a more predictable behavior so that a state modified in previous tests does not
 * influence the result of other tests.
 *
 * To customize any of these behaviours (e.g. to execute only certain initialization scripts), you need to disable the corresponding
 * automatic reset flag and implement your own `@AfterEach` method.
 * If this state reset should concern all tests, it is recommended to define a common superclass, e.g.:
 * ```kotlin
 * class E2ETestBase : Me2eTest() {
 *      @InjectService
 *      private lateinit var database: DatabaseContainer
 *
 *      @AfterEach
 *      fun customStateReset() {
 *          database.clearAllExcept(database.tablesToSkipOnReset)
 *          database.executeScript(database.initializationScripts["script-name"]!!)
 *      }
 * }
 * ```
 */
class Me2eExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    private val logger = logger<Me2eExtension>()

    /**
     * Flag indicating whether this extension is completely disabled.
     * Can be set via the environment variable `ME2E_EXTENSION_DISABLED`.
     */
    private val disabled: Boolean = System.getenv("ME2E_EXTENSION_DISABLED")?.toBoolean() ?: false

    /**
     * Callback function which is executed before the execution of a test class starts.
     * Checks whether the test environment has successfully started. If this is not the case, the current test class
     * and all subsequent test classes are aborted.
     */
    override fun beforeAll(context: ExtensionContext?) {
        if (disabled) return
        if (Me2eTestEnvironmentManager.status != Me2eTestEnvironmentManager.EnvironmentStatus.RUNNING) {
            val message = "The execution of this test class is aborted since the test environment has not started successfully. " +
                "Please check the logs to figure out what when wrong when starting the environment."
            logger.error(message)
            throw TestAbortedException(message)
        }
    }

    /**
     * Callback function which is executed before the execution of a test starts.
     * If activated in the [Me2eTestConfig], it checks whether all Docker containers for which a healthcheck is defined,
     * are currently healthy. If one of the containers is not healthy, an attempt is made to restart the container once.
     * If this attempt fails or if the container is still unhealthy, the test is aborted.
     */
    override fun beforeEach(context: ExtensionContext?) {
        if (disabled) return
        if (Me2eTest.config.settings.assertHealthy) {
            checkContainerHealth()
        }
    }

    /**
     * Callback function which is executed after the execution of a test has finished.
     * If activated in the [Me2eTestConfig], the state of all containers, Mock Servers and databases are reset.
     */
    override fun afterEach(context: ExtensionContext?) {
        if (disabled) return
        if (!Me2eTest.config.settings.stateReset.any()) return
        val exception = context?.executionException?.orElse(null)
        if (exception is TestAbortedException) {
            logger.warn("Skipping state reset since test '${context.displayName}' has been aborted.")
            return
        }
        if (exception != null) {
            logger.warn("Resetting the state may not be possible since the test '${context.displayName}' threw an exception.")
        }
        if (Me2eTest.config.settings.stateReset.clearAllTables) {
            clearDatabases()
        }
        if (Me2eTest.config.settings.stateReset.resetRequestInterceptors) {
            resetRequestInterceptors()
        }
        if (Me2eTest.config.settings.stateReset.resetMockServerRequests) {
            resetMockServerRequests()
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
                "Since they are unhealthy again - even after a restart - the test is aborted."
            logger.warn("$message To disable this behavior, set 'assertHealthy' in the 'TestConfig' to false.")
            throw TestAbortedException(message)
        }
        restartedContainers.addAll(unhealthyContainers)
        try {
            Me2eTest.containerManager.restart(containersToRestart)
        } catch (e: HealthTimeoutException) {
            val message = "Test was aborted since at least one of the containers in the environment did not become healthy after a restart."
            logger.warn("$message To disable this behavior, set 'assertHealthy' in the 'TestConfig' to false.")
            throw TestAbortedException(message, e)
        }
    }

    /**
     * Clears all entries from all tables for all database containers for which a connection to the database
     * is established via a [org.jholsten.me2e.container.database.connection.DatabaseConnection].
     * Skips clearing tables defined in [org.jholsten.me2e.container.database.DatabaseContainer.tablesToSkipOnReset].
     * Executes all initialization scripts defined in [org.jholsten.me2e.container.database.DatabaseContainer.initializationScripts]
     * afterwards.
     */
    private fun clearDatabases() {
        val databaseConnections = Me2eTest.containerManager.databases.values.filter { it.connection != null }
        if (databaseConnections.isNotEmpty()) {
            logger.info("Resetting entries of ${databaseConnections.size} databases.")
            for (database in databaseConnections) {
                clearDatabase(database)
                executeInitializationScripts(database)
            }
        }
    }

    /**
     * Resets request interceptors of all microservice containers.
     */
    private fun resetRequestInterceptors() {
        val microservices = Me2eTest.containerManager.microservices.values
        if (microservices.isNotEmpty()) {
            logger.info("Resetting ${microservices.size} request interceptors of microservice containers.")
            for (microservice in microservices) {
                microservice.resetRequestInterceptors()
            }
        }
    }

    /**
     * Resets all captured requests for all Mock Servers.
     */
    private fun resetMockServerRequests() {
        val mockServers = Me2eTest.mockServerManager.mockServers.values
        if (mockServers.isNotEmpty()) {
            logger.info("Resetting requests of ${mockServers.size} Mock Servers.")
            for (mockServer in mockServers) {
                try {
                    mockServer.reset()
                } catch (e: Exception) {
                    logger.warn("Unable to reset captured requests of Mock Server ${mockServer.name}:", e)
                }
            }
        }
    }

    /**
     * Clears all entries from the given database except the tables defined in
     * [org.jholsten.me2e.container.database.DatabaseContainer.tablesToSkipOnReset].
     * @param database Database for which entries should be cleared.
     */
    private fun clearDatabase(database: DatabaseContainer) {
        try {
            database.clearAllExcept(database.tablesToSkipOnReset)
        } catch (e: Exception) {
            logger.warn("Unable to clear tables of database container ${database.name}:", e)
        }
    }

    /**
     * Executes all initialization scripts defined for the given database in
     * [org.jholsten.me2e.container.database.DatabaseContainer.initializationScripts].
     * @param database Database for which initialization scripts should be executed.
     */
    private fun executeInitializationScripts(database: DatabaseContainer) {
        try {
            database.executeInitializationScripts()
        } catch (e: Exception) {
            logger.warn("Unable to execute initialization scripts of database container ${database.name}:", e)
        }
    }

    /**
     * Returns whether any state property should be reset.
     */
    private fun StateResetConfig.any(): Boolean {
        return clearAllTables || resetRequestInterceptors || resetMockServerRequests
    }

    companion object {
        /**
         * List of Docker container names that have already been restarted once. If one of the containers in this
         * list is unhealthy a second time, no new restart attempt is made and the test is aborted.
         */
        private val restartedContainers: MutableList<String> = mutableListOf()
    }
}
