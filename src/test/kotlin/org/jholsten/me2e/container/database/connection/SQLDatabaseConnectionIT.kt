package org.jholsten.me2e.container.database.connection

import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.database.model.SQLTableSpecification
import org.jholsten.me2e.container.database.model.TableSpecification
import org.jholsten.util.RecursiveComparison
import org.jholsten.util.assertDoesNotThrow
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.testcontainers.containers.GenericContainer
import java.io.File
import java.io.FileNotFoundException
import java.util.stream.Stream
import kotlin.test.*

internal class SQLDatabaseConnectionIT {

    companion object {
        private val postgreSQLContainer = GenericContainer("postgres:12")
            .withEnv(
                mapOf(
                    "POSTGRES_DB" to "testdb",
                    "POSTGRES_USER" to "user",
                    "POSTGRES_PASSWORD" to "123",
                )
            )
            .withExposedPorts(5432)

        private lateinit var postgreSQLConnection: SQLDatabaseConnection

        private val mySQLContainer = GenericContainer("mysql:8.0")
            .withEnv(
                mapOf(
                    "MYSQL_DATABASE" to "testdb",
                    "MYSQL_USER" to "user",
                    "MYSQL_PASSWORD" to "123",
                    "MYSQL_ROOT_PASSWORD" to "secret",
                )
            )
            .withExposedPorts(3306)

        private lateinit var mySQLConnection: SQLDatabaseConnection

        private val mariaDBContainer = GenericContainer("mariadb:11.2.2")
            .withEnv(
                mapOf(
                    "MYSQL_DATABASE" to "testdb",
                    "MYSQL_USER" to "user",
                    "MYSQL_PASSWORD" to "123",
                    "MYSQL_ROOT_PASSWORD" to "secret",
                )
            )
            .withExposedPorts(3306)

        private lateinit var mariaDBConnection: SQLDatabaseConnection

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            postgreSQLContainer.start()
            postgreSQLConnection = SQLDatabaseConnection(
                jdbcUrl = "jdbc:postgresql://${postgreSQLContainer.host}:${postgreSQLContainer.getMappedPort(5432)}/testdb",
                username = "user",
                password = "123",
                system = DatabaseManagementSystem.POSTGRESQL,
            )
            mySQLContainer.start()
            mySQLConnection = SQLDatabaseConnection(
                jdbcUrl = "jdbc:mysql://${mySQLContainer.host}:${mySQLContainer.getMappedPort(3306)}/testdb",
                username = "user",
                password = "123",
                system = DatabaseManagementSystem.MY_SQL,
            )
            mariaDBContainer.start()
            mariaDBConnection = SQLDatabaseConnection(
                jdbcUrl = "jdbc:mysql://${mariaDBContainer.host}:${mariaDBContainer.getMappedPort(3306)}/testdb",
                username = "user",
                password = "123",
                system = DatabaseManagementSystem.MARIA_DB,
            )
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            postgreSQLContainer.stop()
            mySQLContainer.stop()
            mariaDBContainer.stop()
        }

        class DatabaseArgumentProvider : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    Arguments.of(
                        DatabaseArguments(
                            DatabaseManagementSystem.POSTGRESQL,
                            postgreSQLConnection,
                            "public",
                            "database/postgres_script.sql",
                            populate = { populatePostgreSQLDB() },
                        )
                    ),
                    Arguments.of(
                        DatabaseArguments(
                            DatabaseManagementSystem.MY_SQL,
                            mySQLConnection,
                            "testdb",
                            "database/mysql_script.sql",
                            populate = { populateMySQLDB(mySQLConnection) },
                        )
                    ),
                    Arguments.of(
                        DatabaseArguments(
                            DatabaseManagementSystem.MARIA_DB,
                            mariaDBConnection,
                            "testdb",
                            "database/mysql_script.sql",
                            populate = { populateMySQLDB(mariaDBConnection) },
                        )
                    ),
                )
            }
        }

        data class DatabaseArguments(
            val system: DatabaseManagementSystem,
            val connection: SQLDatabaseConnection,
            val schema: String,
            val script: String,
            val populate: () -> Unit,
        ) {
            override fun toString(): String = system.toString()
        }

        private fun populatePostgreSQLDB() {
            val statement = postgreSQLConnection.connection.createStatement()
            statement.executeUpdate("CREATE TABLE company(id INT PRIMARY KEY, name VARCHAR(255))")
            statement.executeUpdate("CREATE TABLE employee(id INT PRIMARY KEY, name VARCHAR(255), company_id INT, FOREIGN KEY (company_id) REFERENCES company ON DELETE CASCADE)")
            statement.executeUpdate("INSERT INTO company(id, name) VALUES (1, 'Company A'), (2, 'Company B')")
            statement.executeUpdate("INSERT INTO employee(id, name, company_id) VALUES (1, 'Employee A.1', 1), (2, 'Employee A.2', 1), (3, 'Employee B.1', 2)")
            statement.close()
        }

        private fun populateMySQLDB(connection: SQLDatabaseConnection) {
            val statement = connection.connection.createStatement()
            statement.execute("USE testdb;")
            statement.executeUpdate("CREATE TABLE company(id INT PRIMARY KEY, name VARCHAR(255))")
            statement.executeUpdate("CREATE TABLE employee(id INT PRIMARY KEY, name VARCHAR(255), company_id INT, FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE)")
            statement.executeUpdate("INSERT INTO company(id, name) VALUES (1, 'Company A'), (2, 'Company B')")
            statement.executeUpdate("INSERT INTO employee(id, name, company_id) VALUES (1, 'Employee A.1', 1), (2, 'Employee A.2', 1), (3, 'Employee B.1', 2)")
            statement.close()
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

    @AfterTest
    fun afterTest() {
        resetPostgreSQLDB()
        resetMySQLDB(mySQLConnection)
        resetMySQLDB(mariaDBConnection)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Executing SQL script should succeed`(databaseArguments: DatabaseArguments) {
        databaseArguments.connection.executeScript(databaseArguments.script)
        assertTablesExist(databaseArguments.connection, databaseArguments.schema)
        RecursiveComparison.assertEquals(expectedCompanies, databaseArguments.connection.getAllFromTable("company"))
        RecursiveComparison.assertEquals(expectedEmployees, databaseArguments.connection.getAllFromTable("employee"))
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Executing non-existing script should fail`(databaseArguments: DatabaseArguments) {
        assertFailsWith<FileNotFoundException> { databaseArguments.connection.executeScript("non-existing") }
        assertFailsWith<FileNotFoundException> { databaseArguments.connection.executeScript(File("non-existing")) }
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing database should succeed`(databaseArguments: DatabaseArguments) {
        databaseArguments.populate()

        databaseArguments.connection.clearAllFromSchema(databaseArguments.schema)

        assertTablesExist(databaseArguments.connection, databaseArguments.schema)
        assertEquals(0, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(0, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing certain tables should succeed`(databaseArguments: DatabaseArguments) {
        databaseArguments.populate()

        databaseArguments.connection.clear(listOf("employee"))

        assertTablesExist(databaseArguments.connection, databaseArguments.schema)
        assertEquals(2, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(0, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing certain tables with empty list should not clean any tables`(databaseArguments: DatabaseArguments) {
        databaseArguments.populate()

        databaseArguments.connection.clear(listOf<String>())

        assertTablesExist(databaseArguments.connection, databaseArguments.schema)
        assertEquals(2, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(3, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing all tables from schema except should skip given tables`(databaseArguments: DatabaseArguments) {
        databaseArguments.populate()

        databaseArguments.connection.clearAllFromSchemaExcept(databaseArguments.schema, listOf(TableSpecification("company")))

        assertTablesExist(databaseArguments.connection, databaseArguments.schema)
        assertEquals(2, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(0, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Statements should be properly rolled back on exception`(databaseArguments: DatabaseArguments) {
        assertFailsWith<Exception> { databaseArguments.connection.getAllFromTable("invalid-table-name") }
        assertDoesNotThrow { databaseArguments.connection.tables }
    }

    private fun assertTablesExist(connection: SQLDatabaseConnection, schema: String) {
        val tables = connection.tables.filter { it.schema == schema }
        assertEquals(2, tables.size)
        assertContains(tables, SQLTableSpecification(name = "company", schema = schema))
        assertContains(tables, SQLTableSpecification(name = "employee", schema = schema))
    }

    private fun resetPostgreSQLDB() {
        val statement = postgreSQLConnection.connection.createStatement()
        statement.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;")
        statement.close()
    }

    private fun resetMySQLDB(connection: SQLDatabaseConnection) {
        val statement = connection.connection.createStatement()
        statement.execute("DROP DATABASE testdb;")
        statement.execute("CREATE DATABASE testdb;")
        statement.close()
    }
}
