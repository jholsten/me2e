package org.jholsten.me2e.container.database

import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import kotlin.test.assertNotNull

internal class DatabaseContainerIT {

    companion object {
        private val sqlDatabase: DatabaseContainer = DatabaseContainer(
            name = "sql-database",
            image = "postgres:12",
            ports = ContainerPortList(
                ports = listOf(ContainerPort(internal = 5432))
            ),
            environment = mapOf(
                "POSTGRES_DB" to "testdb",
                "POSTGRES_USER" to "user",
                "POSTGRES_PASSWORD" to "123",
            ),
            system = DatabaseManagementSystem.POSTGRESQL,
            database = "testdb",
            schema = "public",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_postgres" to "database/postgres_script.sql"),
        )
        private val noSqlDatabase: DatabaseContainer = DatabaseContainer(
            name = "no-sql-database",
            image = "mongo:4.4.27",
            ports = ContainerPortList(
                ports = listOf(ContainerPort(internal = 27017))
            ),
            environment = mapOf(
                "MONGO_INITDB_DATABASE" to "testdb",
                "MONGO_INITDB_ROOT_USERNAME" to "user",
                "MONGO_INITDB_ROOT_PASSWORD" to "123",
            ),
            system = DatabaseManagementSystem.MONGO_DB,
            database = "testdb",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_mongo" to "database/mongo_script_authenticated.js"),
        )

        private val manager = ContainerManager(
            dockerComposeFile = FileUtils.getResourceAsFile("docker-compose-dbs.yml"),
            dockerConfig = DockerConfig(),
            containers = mapOf(
                "sql-database" to sqlDatabase,
                "no-sql-database" to noSqlDatabase,
            )
        )

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            manager.start()
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            manager.stop()
        }

        class DatabaseArgumentProvider : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    Arguments.of("SQL-Database", sqlDatabase),
                    Arguments.of("No-SQL-Database", noSqlDatabase),
                )
            }
        }
    }

    private val expectedCompanies = QueryResult(
        listOf(
            mapOf(
                "id" to 1,
                "name" to "Company A",
            ),
            mapOf(
                "id" to 2,
                "name" to "Company B",
            ),
        )
    )

    private val expectedEmployees = QueryResult(
        listOf(
            mapOf(
                "id" to 1,
                "name" to "Employee A.1",
                "company_id" to 1,
            ),
            mapOf(
                "id" to 2,
                "name" to "Employee A.2",
                "company_id" to 1,
            ),
            mapOf(
                "id" to 3,
                "name" to "Employee B.1",
                "company_id" to 2,
            ),
        )
    )

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    @Suppress("UNUSED_PARAMETER")
    fun `Starting database container should establish database connection`(name: String, databaseContainer: DatabaseContainer) {
        assertNotNull(databaseContainer.connection)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    @Suppress("UNUSED_PARAMETER")
    fun `Starting database container should execute initialization scripts`(name: String, databaseContainer: DatabaseContainer) {
        RecursiveComparison.assertEquals(listOf("company", "employee"), databaseContainer.tables, ignoreCollectionOrder = true)
        val companyResult = databaseContainer.getAllFromTable("company")
        val employeeResult = databaseContainer.getAllFromTable("employee")
        if (databaseContainer.system.isSQL) {
            RecursiveComparison.assertEquals(expectedCompanies, companyResult)
            RecursiveComparison.assertEquals(expectedEmployees, employeeResult)
            RecursiveComparison.assertEquals(listOf("id", "name"), companyResult.columns, ignoreCollectionOrder = true)
            RecursiveComparison.assertEquals(listOf("id", "name", "company_id"), employeeResult.columns, ignoreCollectionOrder = true)
            RecursiveComparison.assertEquals(mapOf("id" to 2, "name" to "Company B"), companyResult.getByField("id", 2))
        } else {
            assertEqualsIgnoreInternalId(expectedCompanies, companyResult)
            assertEqualsIgnoreInternalId(expectedEmployees, employeeResult)
            RecursiveComparison.assertEquals(listOf("_id", "id", "name"), companyResult.columns, ignoreCollectionOrder = true)
            RecursiveComparison.assertEquals(
                listOf("_id", "id", "name", "company_id"),
                employeeResult.columns,
                ignoreCollectionOrder = true
            )
            assertEqualsIgnoreInternalId(mapOf("id" to 2, "name" to "Company B"), companyResult.getByField("id", 2))
        }
        RecursiveComparison.assertEquals(listOf(1, 2), companyResult.getEntriesInColumn("id"))
        RecursiveComparison.assertEquals(listOf("Employee A.1", "Employee A.2", "Employee B.1"), employeeResult.getEntriesInColumn("name"))
    }

    private fun assertEqualsIgnoreInternalId(expected: List<Map<String, Any?>>, actual: List<Map<String, Any?>>) {
        RecursiveComparison.assertEquals(expected, actual.map { e -> e.filterKeys { it != "_id" } })
    }

    private fun assertEqualsIgnoreInternalId(expected: Map<String, Any?>, actual: Map<String, Any?>?) {
        RecursiveComparison.assertEquals(expected, actual?.filterKeys { it != "_id" })
    }
}
