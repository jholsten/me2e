package org.jholsten.me2e.container.database.connection

import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.model.QueryResult
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
            postgreSQLConnection = SQLDatabaseConnection.Builder()
                .withHost(postgreSQLContainer.host)
                .withPort(postgreSQLContainer.getMappedPort(5432))
                .withDatabase("testdb")
                .withUsername("user")
                .withPassword("123")
                .withSystem(DatabaseManagementSystem.POSTGRESQL)
                .build()
            mySQLContainer.start()
            mySQLConnection = SQLDatabaseConnection.Builder()
                .withHost(mySQLContainer.host)
                .withPort(mySQLContainer.getMappedPort(3306))
                .withDatabase("testdb")
                .withUsername("user")
                .withPassword("123")
                .withSystem(DatabaseManagementSystem.MY_SQL)
                .build()
            mariaDBContainer.start()
            mariaDBConnection = SQLDatabaseConnection.Builder()
                .withHost(mariaDBContainer.host)
                .withPort(mariaDBContainer.getMappedPort(3306))
                .withDatabase("testdb")
                .withUsername("user")
                .withPassword("123")
                .withSystem(DatabaseManagementSystem.MARIA_DB)
                .build()
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
                            "database/postgres_script.sql",
                        )
                    ),
                    Arguments.of(
                        DatabaseArguments(
                            DatabaseManagementSystem.MY_SQL,
                            mySQLConnection,
                            "database/mysql_script.sql",
                        )
                    ),
                    Arguments.of(
                        DatabaseArguments(
                            DatabaseManagementSystem.MARIA_DB,
                            mariaDBConnection,
                            "database/mysql_script.sql",
                        )
                    ),
                )
            }
        }

        data class DatabaseArguments(
            val system: DatabaseManagementSystem,
            val connection: SQLDatabaseConnection,
            val script: String,
        ) {
            override fun toString(): String = system.toString()
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

    @Test
    fun `JDBC URLs should be constructed correctly`() {
        val postgreSQLUrl = "${postgreSQLContainer.host}:${postgreSQLContainer.getMappedPort(5432)}"
        assertEquals("jdbc:postgresql://$postgreSQLUrl/testdb", postgreSQLConnection.jdbcUrl)

        val mySQLUrl = "${mySQLContainer.host}:${mySQLContainer.getMappedPort(3306)}"
        assertEquals("jdbc:mysql://$mySQLUrl/testdb", mySQLConnection.jdbcUrl)

        val mariaDBUrl = "${mariaDBContainer.host}:${mariaDBContainer.getMappedPort(3306)}"
        assertEquals("jdbc:mysql://$mariaDBUrl/testdb", mariaDBConnection.jdbcUrl)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Retrieving tables should return all database tables`(databaseArguments: DatabaseArguments) {
        populateDB(databaseArguments.system, databaseArguments.connection)

        val tables = databaseArguments.connection.tables

        RecursiveComparison.assertEquals(listOf("company", "employee"), tables, ignoreCollectionOrder = true)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Retrieving tables for empty database should return empty list`(databaseArguments: DatabaseArguments) {
        val tables = databaseArguments.connection.tables

        assertEquals(0, tables.size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Retrieving entries from table should return all rows`(databaseArguments: DatabaseArguments) {
        populateDB(databaseArguments.system, databaseArguments.connection)

        RecursiveComparison.assertEquals(expectedCompanies, databaseArguments.connection.getAllFromTable("company"))
        RecursiveComparison.assertEquals(expectedEmployees, databaseArguments.connection.getAllFromTable("employee"))
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Executing SQL script should succeed`(databaseArguments: DatabaseArguments) {
        databaseArguments.connection.executeScript(databaseArguments.script)
        assertTablesExist(databaseArguments.connection)
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
        populateDB(databaseArguments.system, databaseArguments.connection)

        databaseArguments.connection.clearAll()

        assertTablesExist(databaseArguments.connection)
        assertEquals(0, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(0, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing certain tables should succeed`(databaseArguments: DatabaseArguments) {
        populateDB(databaseArguments.system, databaseArguments.connection)

        databaseArguments.connection.clear(listOf("employee"))

        assertTablesExist(databaseArguments.connection)
        assertEquals(2, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(0, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing certain tables with empty list should not clean any tables`(databaseArguments: DatabaseArguments) {
        populateDB(databaseArguments.system, databaseArguments.connection)

        databaseArguments.connection.clear(listOf())

        assertTablesExist(databaseArguments.connection)
        assertEquals(2, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(3, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing all tables from schema except should skip given tables`(databaseArguments: DatabaseArguments) {
        populateDB(databaseArguments.system, databaseArguments.connection)

        databaseArguments.connection.clearAllExcept(listOf("company"))

        assertTablesExist(databaseArguments.connection)
        assertEquals(2, databaseArguments.connection.getAllFromTable("company").size)
        assertEquals(0, databaseArguments.connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Statements should be properly rolled back on exception`(databaseArguments: DatabaseArguments) {
        assertFailsWith<Exception> { databaseArguments.connection.getAllFromTable("invalid-table-name") }
        assertDoesNotThrow { databaseArguments.connection.tables }
    }

    private fun assertTablesExist(connection: SQLDatabaseConnection) {
        val tables = connection.tables
        RecursiveComparison.assertEquals(listOf("company", "employee"), tables, ignoreCollectionOrder = true)
    }

    private fun populateDB(system: DatabaseManagementSystem, connection: SQLDatabaseConnection) {
        val statement = connection.connection.createStatement()
        if (system != DatabaseManagementSystem.POSTGRESQL) {
            statement.execute("USE testdb;")
        }
        statement.executeUpdate("CREATE TABLE company(id INT PRIMARY KEY, name VARCHAR(255))")
        if (system == DatabaseManagementSystem.POSTGRESQL) {
            statement.executeUpdate("CREATE TABLE employee(id INT PRIMARY KEY, name VARCHAR(255), company_id INT, FOREIGN KEY (company_id) REFERENCES company ON DELETE CASCADE)")
        } else {
            statement.executeUpdate("CREATE TABLE employee(id INT PRIMARY KEY, name VARCHAR(255), company_id INT, FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE CASCADE)")
        }
        statement.executeUpdate("INSERT INTO company(id, name) VALUES (1, 'Company A'), (2, 'Company B')")
        statement.executeUpdate("INSERT INTO employee(id, name, company_id) VALUES (1, 'Employee A.1', 1), (2, 'Employee A.2', 1), (3, 'Employee B.1', 2)")
        statement.close()
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
