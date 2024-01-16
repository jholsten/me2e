package org.jholsten.me2e.container.database.connection

import org.bson.Document
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.utils.logger
import org.jholsten.util.RecursiveComparison
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.stream.Stream
import kotlin.test.AfterTest
import kotlin.test.assertEquals

internal class MongoDBConnectionTest {

    companion object {
        private val unsecuredContainer = GenericContainer("mongo:7.0.5")
            .withEnv(
                mapOf(
                    "MONGO_INITDB_DATABASE" to "testdb",
                )
            )
            .withExposedPorts(27017)
        //.waitingFor(Wait.forSuccessfulCommand("echo 'db.runCommand(\"ping\").ok' | mongosh --quiet"))

        private lateinit var unsecuredConnection: MongoDBConnection

        private val securedContainer = GenericContainer("mongo:7.0.5")
            .withEnv(
                mapOf(
                    "MONGO_INITDB_DATABASE" to "testdb",
                    "MONGO_INITDB_ROOT_USERNAME" to "user",
                    "MONGO_INITDB_ROOT_PASSWORD" to "123",
                )
            )
            .withExposedPorts(27017)
        //.waitingFor(Wait.forSuccessfulCommand("echo 'db.runCommand(\"ping\").ok' | mongosh -u user -p 123 --quiet"))

        private lateinit var securedConnection: MongoDBConnection

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            try {
                unsecuredContainer.start()
                unsecuredConnection = MongoDBConnection.Builder()
                    .withHost(unsecuredContainer.host)
                    .withPort(unsecuredContainer.getMappedPort(27017))
                    .withDatabase("testdb")
                    .build()
                securedContainer.start()
                securedConnection = MongoDBConnection.Builder()
                    .withHost(securedContainer.host)
                    .withPort(securedContainer.getMappedPort(27017))
                    .withDatabase("testdb")
                    .withUsername("user")
                    .withPassword("123")
                    .build()
            } catch (e: Exception) {
                logger(this).info("GETTING LOGS FROM MONGO1...")
                logger(this).info(unsecuredContainer.logs)
                logger(this).info("LOGS RETRIEVED1")
                logger(this).info("GETTING LOGS FROM MONGO2...")
                logger(this).info(securedContainer.logs)
                logger(this).info("LOGS RETRIEVED2")
                throw e
            }
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            unsecuredConnection.client.close()
            unsecuredContainer.stop()
            securedConnection.client.close()
            securedContainer.stop()
        }

        class DatabaseArgumentProvider : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    Arguments.of(unsecuredConnection, false),
                    Arguments.of(securedConnection, true),
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

    @AfterTest
    fun afterTest() {
        resetDB(unsecuredConnection)
        resetDB(securedConnection)
    }

    @Test
    fun `Connection URL should be constructed correctly`() {
        val unsecuredUrl = "${unsecuredContainer.host}:${unsecuredContainer.getMappedPort(27017)}"
        assertEquals("mongodb://$unsecuredUrl", unsecuredConnection.url)

        val securedUrl = "${securedContainer.host}:${securedContainer.getMappedPort(27017)}"
        assertEquals("mongodb://user:123@$securedUrl", securedConnection.url)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Retrieving tables should return all database tables`(connection: MongoDBConnection, withAuthentication: Boolean) {
        populateDB(connection)

        val tables = connection.tables

        RecursiveComparison.assertEquals(listOf("company", "employee"), tables, ignoreCollectionOrder = true)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Retrieving tables for empty database should return empty list`(connection: MongoDBConnection, withAuthentication: Boolean) {
        val tables = connection.tables

        assertEquals(0, tables.size)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Retrieving entries from table should return all rows`(connection: MongoDBConnection, withAuthentication: Boolean) {
        populateDB(connection)

        val companies = connection.getAllFromTable("company").map { e -> e.filterKeys { it != "_id" } }
        val employees = connection.getAllFromTable("employee").map { e -> e.filterKeys { it != "_id" } }
        RecursiveComparison.assertEquals(expectedCompanies, companies)
        RecursiveComparison.assertEquals(expectedEmployees, employees)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing database should succeed`(connection: MongoDBConnection, withAuthentication: Boolean) {
        populateDB(connection)

        connection.clearAll()

        assertTablesExist(connection)
        assertEquals(0, connection.getAllFromTable("company").size)
        assertEquals(0, connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing certain tables should succeed`(connection: MongoDBConnection, withAuthentication: Boolean) {
        populateDB(connection)

        connection.clear(listOf("employee"))

        assertTablesExist(connection)
        assertEquals(2, connection.getAllFromTable("company").size)
        assertEquals(0, connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing certain tables with empty list should not clean any tables`(connection: MongoDBConnection, withAuthentication: Boolean) {
        populateDB(connection)

        connection.clear(listOf())

        assertTablesExist(connection)
        assertEquals(2, connection.getAllFromTable("company").size)
        assertEquals(3, connection.getAllFromTable("employee").size)
    }

    @ParameterizedTest(name = "[{index}] with authentication: {1}")
    @ArgumentsSource(DatabaseArgumentProvider::class)
    fun `Clearing all tables from schema except should skip given tables`(connection: MongoDBConnection, withAuthentication: Boolean) {
        populateDB(connection)

        connection.clearAllExcept(listOf("company"))

        assertTablesExist(connection)
        assertEquals(2, connection.getAllFromTable("company").size)
        assertEquals(0, connection.getAllFromTable("employee").size)
    }

    private fun assertTablesExist(connection: MongoDBConnection) {
        val tables = connection.tables
        RecursiveComparison.assertEquals(listOf("company", "employee"), tables, ignoreCollectionOrder = true)
    }

    private fun populateDB(connection: MongoDBConnection) {
        connection.connection.createCollection("company")
        val companyCollection = connection.connection.getCollection("company")
        val companies = mutableListOf<Document>()
        for (company in expectedCompanies) {
            val document = Document()
            document["id"] = company["id"]
            document["name"] = company["name"]
            companies.add(document)
        }
        companyCollection.insertMany(companies)

        connection.connection.createCollection("employee")
        val employeeCollection = connection.connection.getCollection("employee")
        val employees = mutableListOf<Document>()
        for (employee in expectedEmployees) {
            val document = Document()
            document["id"] = employee["id"]
            document["name"] = employee["name"]
            document["company_id"] = employee["company_id"]
            employees.add(document)
        }
        employeeCollection.insertMany(employees)
    }

    private fun resetDB(connection: MongoDBConnection) {
        val collections = connection.connection.listCollectionNames().toList()
        for (table in collections) {
            val collection = connection.connection.getCollection(table)
            collection.drop()
        }
    }
}
