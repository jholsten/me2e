package org.jholsten.me2e.container

import io.mockk.*
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.util.RecursiveComparison
import kotlin.test.*

internal class ContainerManagerTest {

    private val dockerComposeFile = FileUtils.getResourceAsFile("docker-compose-parsing-test.yml")
    private val dockerCompose = mockk<DockerCompose>()

    private val manager = ContainerManager(
        dockerComposeFile = dockerComposeFile,
        dockerConfig = DockerConfig(),
        containers = mapOf(
            "backend-api" to MicroserviceContainer(
                name = "backend-api",
                image = "backend-api:latest",
                environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
                requestConfig = RequestConfig(),
                ports = Container.ContainerPortList(
                    ports = listOf(Container.ContainerPort(internal = 8000))
                )
            ),
            "database" to DatabaseContainer(
                name = "database",
                image = "postgres:12",
                environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
                system = DatabaseManagementSystem.POSTGRESQL,
                database = "postgres",
                schema = "public",
                username = "user",
                password = "123",
                initializationScripts = mapOf(),
            ),
            "misc" to Container(
                name = "misc",
                image = "misc:latest",
            ),
        )
    )

    @BeforeTest
    fun beforeTest() {
        mockkConstructor(DockerCompose::class)
        every { anyConstructed<DockerCompose>().withLocalCompose(any()) } returns dockerCompose
        every { anyConstructed<DockerCompose>().withBuild(any()) } returns dockerCompose
        every { anyConstructed<DockerCompose>().withRemoveImages(any()) } returns dockerCompose
        every { anyConstructed<DockerCompose>().withRemoveVolumes(any()) } returns dockerCompose
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Initializing container manager should filter containers`() {
        val expectedMicroservices = mapOf(
            "backend-api" to MicroserviceContainer(
                name = "backend-api",
                image = "backend-api:latest",
                environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
                requestConfig = RequestConfig(),
                ports = Container.ContainerPortList(
                    ports = listOf(Container.ContainerPort(internal = 8000))
                )
            ),
        )
        val expectedDatabases = mapOf(
            "database" to DatabaseContainer(
                name = "database",
                image = "postgres:12",
                environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
                system = DatabaseManagementSystem.POSTGRESQL,
                database = "postgres",
                schema = "public",
                username = "user",
                password = "123",
                initializationScripts = mapOf(),
            ),
        )

        RecursiveComparison.assertEquals(expectedMicroservices, manager.microservices)
        RecursiveComparison.assertEquals(expectedDatabases, manager.databases)
    }
}
