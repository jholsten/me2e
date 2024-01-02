package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.assertions.matchers.StringMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.util.RecursiveComparison
import java.io.FileNotFoundException
import kotlin.test.*

internal class YamlConfigParserIT {

    @Test
    fun `Parsing valid YAML config should succeed`() {
        val config = YamlConfigParser().parseFile("me2e-config-test.yaml")
        assertContainersAsExpected(config)
        assertMockServersAsExpected(config)
        assertRequestConfigAsExpected(RequestConfig(10, 15, 20), config)
    }

    @Test
    fun `Parsing valid YAML config without optional fields should succeed`() {
        val config = YamlConfigParser().parseFile("me2e-config-minimal.yaml")
        assertContainersAsExpected(config)
        assertEquals(0, config.environment.mockServers.size)
        assertRequestConfigAsExpected(RequestConfig(10, 10, 10), config)
    }

    @Test
    fun `Parsing YAML config with invalid format should fail`() {
        assertFailsWith<InvalidFormatException> { YamlConfigParser().parseFile("test-file.txt") }
    }

    @Test
    fun `Parsing YAML config with non-existent Docker-Compose should fail`() {
        assertFailsWith<ParseException> { YamlConfigParser().parseFile("me2e-config-invalid-docker-compose.yaml") }
    }

    @Test
    fun `Parsing non-existent YAML config should fail`() {
        assertFailsWith<FileNotFoundException> { YamlConfigParser().parseFile("non-existent.yaml") }
    }

    private fun assertContainersAsExpected(config: TestConfig) {
        assertEquals(3, config.environment.containers.size)
        assertApiGatewayAsExpected(config.environment.containers)
        assertAuthServerAsExpected(config.environment.containers)
        assertDatabaseAsExpected(config.environment.containers)
    }

    private fun assertMockServersAsExpected(config: TestConfig) {
        assertEquals(1, config.environment.mockServers.size)
        assertPaymentServiceAsExpected(config.environment.mockServers)
    }

    private fun assertRequestConfigAsExpected(expectedRequestConfig: RequestConfig, testConfig: TestConfig) {
        RecursiveComparison.assertEquals(expectedRequestConfig, testConfig.requests)
    }

    private fun assertApiGatewayAsExpected(containers: Map<String, Container>) {
        assertContainerAsExpected(
            containers = containers,
            name = "api-gateway",
            type = ContainerType.MICROSERVICE,
            image = "api-gateway-service:latest",
            environment = mapOf(
                "DB_PASSWORD" to "123",
                "DB_USER" to "user",
            ),
            public = true,
            containerPorts = listOf(1234, 80, 1200, 1201, 1202),
        )
    }

    private fun assertAuthServerAsExpected(containers: Map<String, Container>) {
        assertContainerAsExpected(
            containers = containers,
            name = "auth-server",
            type = ContainerType.MICROSERVICE,
            image = "auth-server:1.3.0",
            environment = mapOf(
                "ADMIN_PASSWORD" to "secret",
            ),
            public = true,
            containerPorts = listOf(),
        )
    }

    private fun assertDatabaseAsExpected(containers: Map<String, Container>) {
        assertContainerAsExpected(
            containers = containers,
            name = "database",
            type = ContainerType.DATABASE,
            image = "postgres:12",
            environment = mapOf(
                "DB_PASSWORD" to "123",
                "DB_USER" to "user",
            ),
            public = false,
            containerPorts = listOf(),
            databaseSystem = DatabaseManagementSystem.POSTGRESQL,
        )
    }

    private fun assertContainerAsExpected(
        containers: Map<String, Container>,
        name: String,
        type: ContainerType,
        image: String,
        environment: Map<String, String>?,
        public: Boolean,
        containerPorts: List<Int>,
        databaseSystem: DatabaseManagementSystem? = null,
    ) {
        val container = containers[name]
        assertNotNull(container)
        when (type) {
            ContainerType.MICROSERVICE -> assertIs<MicroserviceContainer>(container)
            ContainerType.DATABASE -> assertIs<DatabaseContainer>(container)
            ContainerType.MISC -> assertIs<Container>(container)
        }
        assertEquals(name, container.name)
        assertEquals(type, container.type)
        assertEquals(image, container.image)
        assertEquals(environment, container.environment)
        assertEquals(public, container.public)
        assertEquals(containerPorts.size, container.ports.size)
        for (i in containerPorts.indices) {
            assertEquals(containerPorts[i], container.ports[i].internal)
        }
        if (container is DatabaseContainer) {
            assertEquals(databaseSystem, container.system)
        }
    }

    private fun assertPaymentServiceAsExpected(mockServers: Map<String, MockServer>) {
        assertMockServerAsExpected(
            mockServers = mockServers,
            name = "payment-service",
            hostname = "payment.example.com",
            stubs = listOf(
                MockServerStub(
                    name = "request-stub",
                    request = MockServerStubRequestMatcher(
                        hostname = "payment.example.com",
                        method = HttpMethod.POST,
                        path = StringMatcher(equals = "/search"),
                        bodyPatterns = listOf(StringMatcher(contains = "\"id\": 123")),
                    ),
                    response = MockServerStubResponse(
                        code = 200,
                        body = MockServerStubResponseBody(
                            jsonContent = JsonNodeFactory.instance.objectNode()
                                .put("id", 123)
                                .set<ObjectNode>(
                                    "items", JsonNodeFactory.instance.arrayNode()
                                        .add(JsonNodeFactory.instance.objectNode().put("name", "A").put("value", 42))
                                        .add(JsonNodeFactory.instance.objectNode().put("name", "B").put("value", 1))
                                ),
                        ),
                        headers = mapOf("Content-Type" to listOf("application/json")),
                    )
                )
            )
        )
    }

    private fun assertMockServerAsExpected(
        mockServers: Map<String, MockServer>,
        name: String,
        hostname: String,
        stubs: List<MockServerStub>
    ) {
        val mockServer = mockServers[name]
        assertNotNull(mockServer)
        assertEquals(name, mockServer.name)
        assertEquals(hostname, mockServer.hostname)
        RecursiveComparison.assertEquals(stubs, mockServer.stubs)
    }
}
