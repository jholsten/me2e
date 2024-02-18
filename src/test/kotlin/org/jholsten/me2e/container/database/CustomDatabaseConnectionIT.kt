package org.jholsten.me2e.container.database

import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.database.connection.DatabaseConnection
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.AfterAll
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CustomDatabaseConnectionIT {

    companion object {
        private val testConfig = ConfigFormat.YAML.parser.parseFile("me2e-config-custom-db-connection.yaml")

        private val manager = ContainerManager(
            dockerComposeFile = FileUtils.getResourceAsFile("docker-compose-custom-db-connection.yml"),
            dockerConfig = testConfig.settings.docker,
            containers = testConfig.environment.containers,
        )

        private val database = manager.databases["database"]!!

        @AfterAll
        @JvmStatic
        fun afterAll() {
            manager.stop()
        }
    }

    @Test
    fun `Defining custom database connection class should instantiate custom implementation`() {
        manager.start()

        assertNotNull(database.connection)
        assertIs<CustomDatabaseConnection>(database.connection)
        assertEquals(listOf("table1", "table2"), database.tables)
    }
}

open class CustomDatabaseConnection(host: String, port: Int, database: String, username: String?, password: String?) : DatabaseConnection(
    host = host,
    port = port,
    database = database,
    username = username,
    password = password,
    system = DatabaseManagementSystem.OTHER,
) {
    private val logger = logger<CustomDatabaseConnection>()

    override val tables: List<String>
        get() = listOf("table1", "table2")

    override fun getAllFromTable(tableName: String): QueryResult {
        return QueryResult(rows = listOf(mapOf("column1" to "value1")))
    }

    override fun executeScript(name: String?, file: File) {
        logger.info("Executing script.")
    }

    override fun clear(tablesToClear: List<String>) {
        logger.info("Clearing tables.")
    }

    override fun reset() {
        logger.info("Resetting database.")
    }

    override fun close() {
        logger.info("Closing connection.")
    }

    open class Builder : DatabaseConnection.Builder<Builder>() {
        override fun build(): DatabaseConnection {
            return CustomDatabaseConnection(
                host = requireNotNull(host),
                port = requireNotNull(port),
                database = requireNotNull(database),
                username = username,
                password = password,
            )
        }
    }
}
