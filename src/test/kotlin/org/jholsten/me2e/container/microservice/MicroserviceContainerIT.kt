package org.jholsten.me2e.container.microservice

import org.jholsten.me2e.assertions.Assertions.Companion.assertThat
import org.jholsten.me2e.assertions.Assertions.Companion.isEqualTo
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.request.model.RelativeUrl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MicroserviceContainerIT {

    private val manager = ContainerManager(
        dockerComposeFile = FileUtils.getResourceAsFile("docker-compose.yml"),
        dockerConfig = DockerConfig(),
        containers = mapOf(
            "backend-api" to MicroserviceContainer(
                name = "backend-api",
                image = "gitlab.informatik.uni-bremen.de:5005/master-thesis1/test-system/backend-api:latest",
                environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
                requestConfig = RequestConfig(),
                ports = Container.ContainerPortList(
                    ports = listOf(Container.ContainerPort(internal = 8000))
                ),
                hasHealthcheck = true,
            ),
            "database" to DatabaseContainer(
                name = "database",
                image = "postgres:12",
                environment = mapOf("POSTGRES_PASSWORD" to "123", "POSTGRES_USER" to "user"),
                system = DatabaseManagementSystem.POSTGRESQL,
            ),
        )
    )

    @BeforeAll
    fun beforeAll() {
        manager.start()
    }

    @AfterAll
    fun afterAll() {
        manager.stop()
    }

    @Test
    fun `Executing GET request should succeed`() {
        val backendApi = manager.microservices["backend-api"]!!

        val response = backendApi.get(RelativeUrl("/health"))

        assertThat(response).statusCode(isEqualTo(200))
        assertThat(response).body(isEqualTo("OK"))
    }
}
