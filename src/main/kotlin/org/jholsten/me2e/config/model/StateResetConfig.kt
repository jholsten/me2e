package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.Me2eExtension

/**
 * Configuration for resetting the state of containers, Mock Servers and databases after each test.
 * @constructor Instantiates a new configuration for resetting the state.
 * @param clearAllTables Whether to clear all entries from all tables for all database containers for which a
 * connection to the database is established via a [org.jholsten.me2e.container.database.connection.DatabaseConnection]
 * after each test. Tables defined in [org.jholsten.me2e.container.database.DatabaseContainer.tablesToSkipOnReset] will
 * be excluded and are not cleared. Note that after clearing the database, all initialization scripts defined in
 * [org.jholsten.me2e.container.database.DatabaseContainer.initializationScripts] are executed to restore the original
 * state of the database.
 * @param resetRequestInterceptors Whether to reset all request interceptors of all microservice containers after each test.
 * @param resetMockServerRequests Whether to reset all captured requests for all Mock Servers after each test.
 */
data class StateResetConfig internal constructor(
    /**
     * Whether to clear all entries from all tables for all database containers for which a connection to the
     * database is established via a [org.jholsten.me2e.container.database.connection.DatabaseConnection] after
     * each test. Tables defined in [org.jholsten.me2e.container.database.DatabaseContainer.tablesToSkipOnReset]
     * will be excluded and are not cleared. Note that after clearing the database, all initialization scripts
     * defined in [org.jholsten.me2e.container.database.DatabaseContainer.initializationScripts] are executed to
     * restore the original state of the database.
     * @see Me2eExtension.clearDatabases
     */
    @JsonProperty("clear-all-tables")
    val clearAllTables: Boolean = true,

    /**
     * Whether to reset all request interceptors of all microservice containers after each test.
     * @see Me2eExtension.resetRequestInterceptors
     */
    @JsonProperty("reset-request-interceptors")
    val resetRequestInterceptors: Boolean = true,

    /**
     * Whether to reset all captured requests for all Mock Servers after each test.
     * @see Me2eExtension.resetMockServerRequests
     */
    @JsonProperty("reset-mock-server-requests")
    val resetMockServerRequests: Boolean = true,
)
