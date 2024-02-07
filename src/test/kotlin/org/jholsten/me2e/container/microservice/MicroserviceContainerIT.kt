package org.jholsten.me2e.container.microservice

import org.jholsten.me2e.assertions.assertThat
import org.jholsten.me2e.assertions.equalTo
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.container.ContainerManager
import org.jholsten.me2e.container.microservice.authentication.UsernamePasswordAuthentication
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.request.model.RelativeUrl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test

internal class MicroserviceContainerIT {

    companion object {
        private val backendApi = MicroserviceContainer(
            name = "backend-api",
            image = "gitlab.informatik.uni-bremen.de:5005/master-thesis1/test-system/backend-api:latest",
            environment = mapOf("DB_PASSWORD" to "123", "DB_USER" to "user"),
            requestConfig = RequestConfig(),
            ports = ContainerPortList(
                ports = listOf(ContainerPort(internal = 8000))
            ),
            hasHealthcheck = true,
        )

        private val manager = ContainerManager(
            dockerComposeFile = FileUtils.getResourceAsFile("docker-compose.yml"),
            dockerConfig = DockerConfig(),
            containers = mapOf("backend-api" to backendApi)
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
    }

    @Test
    fun `Executing GET request should succeed`() {
        val response = backendApi.get(RelativeUrl("/health"))

        assertThat(response).statusCode(equalTo(200))
        assertThat(response).body(equalTo("OK"))
    }

    @Test
    fun `Executing authenticated GET request should succeed`() {
        backendApi.authenticate(UsernamePasswordAuthentication("admin", "secret"))

        val response = backendApi.get(RelativeUrl("/secured"))

        assertThat(response).statusCode(equalTo(200))
        assertThat(response).body(equalTo("admin"))
        backendApi.resetRequestInterceptors()
    }
}
