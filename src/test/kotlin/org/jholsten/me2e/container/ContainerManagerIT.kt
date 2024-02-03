package org.jholsten.me2e.container

import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.parsing.utils.FileUtils
import org.junit.jupiter.api.Nested
import kotlin.test.*

internal class ContainerManagerIT {

    private val manager = ContainerManager(
        dockerComposeFile = FileUtils.getResourceAsFile("docker-compose.yml"),
        dockerConfig = DockerConfig(),
        containers = mapOf(
            "backend-api" to MicroserviceContainer(
                name = "backend-api",
                image = "gitlab.informatik.uni-bremen.de:5005/master-thesis1/test-system/backend-api:latest",
                environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
                requestConfig = RequestConfig(),
                ports = ContainerPortList(
                    ports = listOf(ContainerPort(internal = 8000))
                ),
                hasHealthcheck = true,
            ),
            "database" to DatabaseContainer(
                name = "database",
                image = "postgres:12",
                environment = mapOf("POSTGRES_PASSWORD" to "123", "POSTGRES_USER" to "user"),
                system = DatabaseManagementSystem.POSTGRESQL,
                database = "postgres",
                schema = "public",
                username = "user",
                password = "123",
                initializationScripts = mapOf(),
            ),
        )
    )

    @Test
    fun `Starting container manager should start all containers`() {
        manager.start()

        for (container in manager.containers.values) {
            assertTrue(container.isRunning)
            assertEquals(container.hasHealthcheck, container.isHealthy)
        }
        manager.stop()
    }

    @Nested
    inner class RunningContainerManagerIT {
        @BeforeTest
        fun beforeTest() {
            manager.start()
        }

        @Test
        fun `Stopping container manager should stop all containers`() {
            manager.stop()

            for (container in manager.containers.values) {
                assertFalse(container.isRunning)
                assertFalse(container.isHealthy)
            }
        }
    }
}
