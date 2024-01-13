package org.jholsten.me2e

import org.jholsten.me2e.assertions.Assertions.Companion.assertThat
import org.jholsten.me2e.assertions.Assertions.Companion.isEqualTo
import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.injection.InjectContainer
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.MediaType
import org.jholsten.me2e.request.model.RelativeUrl
import org.junit.jupiter.api.AfterAll
import kotlin.test.*

@Me2eTestConfig(
    config = "me2e-config-test.yaml",
    format = ConfigFormat.YAML,
)
class Me2eTestIT : Me2eTest() {

    @InjectContainer
    private lateinit var backendApi: MicroserviceContainer

    @InjectContainer("backend-api")
    private lateinit var backendApiAlt: MicroserviceContainer

    @InjectContainer
    private lateinit var database: Container

    companion object {
        @JvmStatic
        @AfterAll
        fun afterAll() {
            mockServerManager.stop()
            containerManager.stop()
        }
    }

    // TODO: Inject Mockserver
    // TODO: Add annotation processor for verifying datatype + superclass

    @Test
    fun `Initializing test class should start environment and inject containers`() {
        assertEquals("me2e-config-test.yaml", configAnnotation.config)
        assertEquals(ConfigFormat.YAML, configAnnotation.format)
        for (container in containerManager.containers.values) {
            assertTrue(container.isRunning)
        }
        assertTrue(mockServerManager.isRunning)
        for (mockServer in mockServerManager.mockServers.values) {
            assertTrue(mockServer.isRunning)
        }
        assertNotNull(backendApi)
        assertSame(containerManager.containers["backend-api"], backendApi)
        assertNotNull(backendApiAlt)
        assertSame(containerManager.containers["backend-api"], backendApiAlt)
        assertNotNull(database)
        assertSame(containerManager.containers["database"], database)
    }

    @Test
    fun `Invoking mock server in container should succeed`() {
        val response = backendApi.post(RelativeUrl("/search"), HttpRequestBody(content = "{\"id\": 123}", MediaType.JSON_UTF8))

        println(backendApi.getLogs())
        assertThat(response).statusCode(isEqualTo(200))
        assertThat(response).jsonBody("id", isEqualTo("123"))
        assertThat(response).jsonBody("items[0].name", isEqualTo("A"))
        assertThat(response).jsonBody("items[0].value", isEqualTo("42"))
        assertThat(response).jsonBody("items[1].name", isEqualTo("B"))
        assertThat(response).jsonBody("items[1].value", isEqualTo("1"))
        // TODO: Verify mock server
    }
}
