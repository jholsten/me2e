package org.jholsten.me2e

import org.jholsten.me2e.assertions.assertThat
import org.jholsten.me2e.assertions.containsNode
import org.jholsten.me2e.assertions.equalTo
import org.jholsten.me2e.config.model.ConfigFormat
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.injection.InjectService
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.verification.ExpectedRequest
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.me2e.request.model.HttpRequestBody
import org.jholsten.me2e.request.model.ContentType
import org.jholsten.me2e.request.model.RelativeUrl
import org.jholsten.me2e.utils.logger
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.*

@Me2eTestConfig(
    config = "me2e-config-test.yaml",
    format = ConfigFormat.YAML,
)
class Me2eTestIT : Me2eTest() {

    private val logger = logger<Me2eTestIT>()

    @InjectService
    private lateinit var backendApi: MicroserviceContainer

    @InjectService("backend-api")
    private lateinit var backendApiAlt: MicroserviceContainer

    @InjectService
    private lateinit var database: Container

    @InjectService
    private lateinit var paymentService: MockServer

    @InjectService("payment-service")
    private lateinit var paymentServiceAlt: MockServer

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            mockServerManager.start()
            containerManager.start()
        }
        
        @JvmStatic
        @AfterAll
        fun afterAll() {
            mockServerManager.stop()
            containerManager.stop()
        }
    }

    @Test
    fun `Initializing test class should start environment and inject services`() {
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
        assertNotNull(paymentService)
        assertSame(mockServerManager.mockServers["payment-service"], paymentService)
        assertNotNull(paymentServiceAlt)
        assertSame(mockServerManager.mockServers["payment-service"], paymentServiceAlt)
    }

    @ParameterizedTest(name = "[{index}] with {1}")
    @CsvSource(
        "/search, HTTP",
        "/https, HTTPS",
    )
    @Suppress("UNUSED_PARAMETER")
    fun `Invoking Mock Server in container should succeed`(relativeUrl: String, description: String) {
        val response = backendApi.post(RelativeUrl(relativeUrl), HttpRequestBody(content = "{\"id\": 123}", ContentType.JSON_UTF8))

        logger.info(backendApi.getLogs().toString())
        assertThat(response).statusCode(equalTo(200))
        assertThat(response).jsonBody(containsNode("id").withValue(equalTo("123")))
        assertThat(response).jsonBody(containsNode("items[0].name").withValue(equalTo("A")))
        assertThat(response).jsonBody(containsNode("items[0].value").withValue(equalTo("42")))
        assertThat(response).jsonBody(containsNode("items[1].name").withValue(equalTo("B")))
        assertThat(response).jsonBody(containsNode("items[1].value").withValue(equalTo("1")))

        assertThat(paymentService).receivedRequest(
            ExpectedRequest()
                .withPath(equalTo("/search"))
                .withMethod(equalTo(HttpMethod.POST))
                .withBody(equalTo("{\"id\": 123}"))
                .andNoOther()
        )
    }

    @Nested
    inner class NestedMe2eTest {
        @Test
        fun `Accessing injected services in nested class should succeed`() {
            assertNotNull(backendApi)
            assertSame(containerManager.containers["backend-api"], backendApi)
        }
    }
}
