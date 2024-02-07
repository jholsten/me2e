package org.jholsten.me2e.container.database

import com.github.dockerjava.api.command.EventsCmd
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.model.ContainerPort as DockerContainerPort
import io.mockk.*
import com.github.dockerjava.api.model.Container as DockerContainer
import org.jholsten.me2e.container.database.connection.MongoDBConnection
import org.jholsten.me2e.container.database.connection.SQLDatabaseConnection
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.testcontainers.containers.ContainerState
import java.io.File
import java.util.stream.Stream
import kotlin.test.*

internal class DatabaseContainerTest {

    companion object {
        private val mockedDockerContainer = mockk<DockerContainer>()
        private val mockedDockerContainerState = mockk<ContainerState>()
        private val mockedEnvironment = mockk<DockerCompose>()
        private val mockedSQLConnection = mockk<SQLDatabaseConnection>()
        private val mockedMongoDBConnection = mockk<MongoDBConnection>()

        private val postgreSQLContainer = DatabaseContainer(
            name = "postgres-db",
            image = "postgres:12",
            environment = mapOf(),
            ports = ContainerPortList(listOf(ContainerPort(internal = 5432))),
            hasHealthcheck = true,
            system = DatabaseManagementSystem.POSTGRESQL,
            database = "testdb",
            schema = "public",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_1" to "database/init_1.sql", "init_2" to "database/init_2.sql"),
        )

        private val mySQLContainer = DatabaseContainer(
            name = "my-sql-db",
            image = "mysql:8.0",
            environment = mapOf(),
            ports = ContainerPortList(listOf(ContainerPort(internal = 3306))),
            hasHealthcheck = true,
            system = DatabaseManagementSystem.MY_SQL,
            database = "testdb",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_1" to "database/init_1.sql", "init_2" to "database/init_2.sql"),
        )

        private val mariaDBContainer = DatabaseContainer(
            name = "maria-db",
            image = "mariadb:11.2.2",
            environment = mapOf(),
            ports = ContainerPortList(listOf(ContainerPort(internal = 3306))),
            hasHealthcheck = true,
            system = DatabaseManagementSystem.MARIA_DB,
            database = "testdb",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_1" to "database/init_1.sql", "init_2" to "database/init_2.sql"),
        )

        private val mongoDBContainer = DatabaseContainer(
            name = "mongo-db",
            image = "mongo:4.4.27",
            environment = mapOf(),
            ports = ContainerPortList(listOf(ContainerPort(internal = 27017))),
            hasHealthcheck = true,
            system = DatabaseManagementSystem.MONGO_DB,
            database = "testdb",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_1" to "database/init_1.sql", "init_2" to "database/init_2.sql"),
        )

        private val otherDBContainer = DatabaseContainer(
            name = "other-db",
            image = "unknown-db:latest",
            environment = mapOf(),
            ports = ContainerPortList(listOf(ContainerPort(internal = 12345))),
            hasHealthcheck = true,
            system = DatabaseManagementSystem.OTHER,
            database = "testdb",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_1" to "database/init_1.sql", "init_2" to "database/init_2.sql"),
        )

        class SQLContainerArgumentProvider : ArgumentsProvider {
            override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
                return Stream.of(
                    Arguments.of(DatabaseManagementSystem.POSTGRESQL, postgreSQLContainer),
                    Arguments.of(DatabaseManagementSystem.MY_SQL, mySQLContainer),
                    Arguments.of(DatabaseManagementSystem.MARIA_DB, mariaDBContainer),
                )
            }

        }
    }

    @BeforeTest
    fun beforeTest() {
        mockkConstructor(SQLDatabaseConnection.Builder::class)
        mockkConstructor(MongoDBConnection.Builder::class)
        every { anyConstructed<SQLDatabaseConnection.Builder>().build() } returns mockedSQLConnection
        every { anyConstructed<MongoDBConnection.Builder>().build() } returns mockedMongoDBConnection
        every { mockedDockerContainer.getPorts() } returns arrayOf(
            DockerContainerPort().withIp("127.0.0.1").withType("tcp").withPrivatePort(5432).withPublicPort(5432),
            DockerContainerPort().withIp("127.0.0.1").withType("tcp").withPrivatePort(3306).withPublicPort(3306),
            DockerContainerPort().withIp("127.0.0.1").withType("tcp").withPrivatePort(27017).withPublicPort(27017),
            DockerContainerPort().withIp("127.0.0.1").withType("tcp").withPrivatePort(12345).withPublicPort(12345),
        )
        every { mockedDockerContainerState.host } returns "localhost"
        every { mockedDockerContainerState.containerId } returns "containerId"
        every { mockedDockerContainerState.dockerClient } returns mockk {
            every { logContainerCmd(any()) } returns mockk<LogContainerCmd> {
                every { withFollowStream(any()) } returns this
                every { withSince(any()) } returns this
                every { withStdOut(any()) } returns this
                every { withStdErr(any()) } returns this
                every { exec(any()) } returns mockk()
            }
            every { eventsCmd() } returns mockk<EventsCmd> {
                every { withContainerFilter(any()) } returns this
                every { withEventFilter(any()) } returns this
                every { exec(any()) } returns mockk()
            }
        }
        every { mockedSQLConnection.tables } returns listOf("company")
        every { mockedSQLConnection.getAllFromTable(any()) } returns QueryResult(listOf(mapOf("id" to "1")))
        every { mockedSQLConnection.executeScript(any(), any<File>()) } just runs
        every { mockedSQLConnection.executeScript(any(), any<String>()) } just runs
        every { mockedSQLConnection.clear(any()) } just runs
        every { mockedSQLConnection.clearAll() } just runs
        every { mockedSQLConnection.clearAllExcept(any()) } just runs
        every { mockedSQLConnection.reset() } just runs
        every { mockedMongoDBConnection.tables } returns listOf("company")
        every { mockedMongoDBConnection.getAllFromTable(any()) } returns QueryResult(listOf(mapOf("id" to "1")))
        every { mockedMongoDBConnection.executeScript(any(), any<File>()) } just runs
        every { mockedMongoDBConnection.executeScript(any(), any<String>()) } just runs
        every { mockedMongoDBConnection.clear(any()) } just runs
        every { mockedMongoDBConnection.clearAll() } just runs
        every { mockedMongoDBConnection.clearAllExcept(any()) } just runs
        every { mockedMongoDBConnection.reset() } just runs
        mockkObject(ReportDataAggregator)
        every { ReportDataAggregator.onContainerStarted(any()) } just runs
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @ParameterizedTest(name = "[{index}] with {0}")
    @ArgumentsSource(SQLContainerArgumentProvider::class)
    @Suppress("UNUSED_PARAMETER")
    fun `Initializing database container should initialize connection`(system: DatabaseManagementSystem, container: DatabaseContainer) {
        container.initializeOnContainerStarted(mockedDockerContainer, mockedDockerContainerState, mockedEnvironment)

        assertNotNull(container.connection)
        verify { mockedSQLConnection.executeScript("init_1", "database/init_1.sql") }
        verify { mockedSQLConnection.executeScript("init_2", "database/init_2.sql") }
    }
}
