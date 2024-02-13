package org.jholsten.me2e

import org.jholsten.me2e.config.model.StateResetConfig
import org.jholsten.me2e.config.model.TestSettings
import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit5 extension which - if activated in the [TestSettings.stateReset] - resets the state of all
 *  containers, Mock Servers and databases after each test. Includes the following states:
 * - [org.jholsten.me2e.container.database.DatabaseContainer]: Database entries
 * - [org.jholsten.me2e.container.microservice.MicroserviceContainer]: Request Interceptors
 * - [org.jholsten.me2e.mock.MockServer]: Received requests
 *
 * Resetting the state after each test ensures a more predictable behavior so that a state modified in
 * previous tests does not influence the result of other tests.
 */
class Me2eStateResetExtension internal constructor() : AfterEachCallback {
    private val logger = logger<Me2eStateResetExtension>()

    /**
     * Callback function which is executed after the execution of a test has finished.
     * If activated in the [Me2eTestConfig], the state of all containers, Mock Servers and databases are reset.
     */
    override fun afterEach(context: ExtensionContext?) {
        if (Me2eTest.config.settings.stateReset.any() && context?.executionException?.isPresent == true) {
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
     * Clears all entries from all tables for all database containers for which a connection to the database
     * is established via a [org.jholsten.me2e.container.database.connection.DatabaseConnection].
     * Skips clearing tables defined in [org.jholsten.me2e.container.database.DatabaseContainer.tablesToSkipOnReset].
     */
    private fun clearDatabases() {
        val databaseConnections = Me2eTest.containerManager.databases.values
            .filter { it.connection != null }
            .map { it to it.connection }
        if (databaseConnections.isNotEmpty()) {
            logger.debug("Resetting entries of ${databaseConnections.size} databases.")
            for ((database, connection) in databaseConnections) {
                try {
                    connection!!.clearAllExcept(database.tablesToSkipOnReset)
                } catch (e: Exception) {
                    logger.warn("Unable to clear tables of database container ${database.name}:", e)
                }
            }
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

    /**
     * Resets all captured requests for all Mock Servers.
     */
    private fun resetMockServerRequests() {
        val mockServers = Me2eTest.mockServerManager.mockServers.values
        if (mockServers.isNotEmpty()) {
            logger.debug("Resetting requests of ${mockServers.size} Mock Servers.")
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
     * Returns whether any state property should be reset.
     */
    private fun StateResetConfig.any(): Boolean {
        return clearAllTables || resetRequestInterceptors || resetMockServerRequests
    }
}
