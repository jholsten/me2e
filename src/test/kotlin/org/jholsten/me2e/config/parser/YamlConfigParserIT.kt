package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.container.docker.DockerComposeVersion
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.MockServerConfig
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.config.model.DockerConfig.DockerComposeRemoveImagesStrategy
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.mock.stubbing.MockServerStub
import org.jholsten.me2e.mock.stubbing.request.MockServerStubRequestMatcher
import org.jholsten.me2e.mock.stubbing.request.StringMatcher
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponse
import org.jholsten.me2e.mock.stubbing.response.MockServerStubResponseBody
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.request.model.HttpMethod
import org.jholsten.util.RecursiveComparison
import java.io.FileNotFoundException
import kotlin.test.*

internal class YamlConfigParserIT {

    @Test
    fun `Parsing valid YAML config should succeed`() {
        val config = YamlConfigParser().parseFile("me2e-config-parsing-test.yaml")
        assertContainersAsExpected(config)
        assertMockServersAsExpected(config)
        assertRequestConfigAsExpected(RequestConfig(10, 15, 20, false), config)
        assertDockerConfigAsExpected(
            DockerConfig(
                dockerComposeVersion = DockerComposeVersion.V1,
                pullPolicy = DockerConfig.PullPolicy.ALWAYS,
                buildImages = true,
                removeImages = DockerComposeRemoveImagesStrategy.ALL,
                removeVolumes = false,
                healthTimeout = 30,
            ), config
        )
        assertMockServerConfigAsExpected(
            MockServerConfig(
                keystorePath = "keystore.jks",
                keystorePassword = "keystore-password",
                keyManagerPassword = "key-manager-password",
                keystoreType = "BKS",
                truststorePath = "truststore.jks",
                truststorePassword = "truststore-password",
                truststoreType = "BKS",
                needsClientAuth = true,
            ), config
        )
    }

    @Test
    fun `Parsing valid YAML config without optional fields should succeed`() {
        val config = YamlConfigParser().parseFile("me2e-config-minimal.yaml")
        assertContainersAsExpected(config, pullPolicy = DockerConfig.PullPolicy.MISSING)
        assertEquals(0, config.environment.mockServers.size)
        assertRequestConfigAsExpected(RequestConfig(10, 10, 10), config)
    }

    @Test
    fun `Parsing YAML config with invalid format should fail`() {
        assertFailsWith<InvalidFormatException> { YamlConfigParser().parseFile("test-file.txt") }
    }

    @Test
    fun `Parsing YAML config with non-existent Docker-Compose should fail`() {
        assertFailsWith<ParseException> { YamlConfigParser().parseFile("me2e-config-non-existent-docker-compose.yaml") }
    }

    @Test
    fun `Parsing YAML config with invalid Docker-Compose should fail`() {
        assertFailsWith<ValidationException> { YamlConfigParser().parseFile("me2e-config-invalid-docker-compose.yaml") }
    }

    @Test
    fun `Parsing non-existent YAML config should fail`() {
        assertFailsWith<FileNotFoundException> { YamlConfigParser().parseFile("non-existent.yaml") }
    }

    private fun assertContainersAsExpected(config: TestConfig, pullPolicy: DockerConfig.PullPolicy? = null) {
        assertEquals(4, config.environment.containers.size)
        assertApiGatewayAsExpected(config.environment.containers, pullPolicy)
        assertAuthServerAsExpected(config.environment.containers, pullPolicy)
        assertSQLDatabaseAsExpected(config.environment.containers, pullPolicy)
        assertNoSQLDatabaseAsExpected(config.environment.containers, pullPolicy)
    }

    private fun assertMockServersAsExpected(config: TestConfig) {
        assertEquals(1, config.environment.mockServers.size)
        assertPaymentServiceAsExpected(config.environment.mockServers)
    }

    private fun assertDockerConfigAsExpected(expectedDockerConfig: DockerConfig, testConfig: TestConfig) {
        RecursiveComparison.assertEquals(expectedDockerConfig, testConfig.settings.docker)
    }

    private fun assertRequestConfigAsExpected(expectedRequestConfig: RequestConfig, testConfig: TestConfig) {
        RecursiveComparison.assertEquals(expectedRequestConfig, testConfig.settings.requests)
    }

    private fun assertApiGatewayAsExpected(containers: Map<String, Container>, pullPolicy: DockerConfig.PullPolicy? = null) {
        assertContainerAsExpected(
            containers = containers,
            name = "api-gateway",
            type = ContainerType.MICROSERVICE,
            image = null,
            environment = mapOf(
                "DB_PASSWORD" to "123",
                "DB_USER" to "user",
            ),
            pullPolicy = pullPolicy ?: DockerConfig.PullPolicy.ALWAYS,
            hasHealthcheck = true,
            containerPorts = listOf(1234, 80, 1200, 1201, 1202),
        )
    }

    private fun assertAuthServerAsExpected(containers: Map<String, Container>, pullPolicy: DockerConfig.PullPolicy? = null) {
        assertContainerAsExpected(
            containers = containers,
            name = "auth-server",
            type = ContainerType.MICROSERVICE,
            image = "auth-server:1.3.0",
            environment = mapOf(
                "ADMIN_PASSWORD" to "secret",
            ),
            pullPolicy = pullPolicy ?: DockerConfig.PullPolicy.MISSING,
            hasHealthcheck = false,
            containerPorts = listOf(),
        )
    }

    private fun assertSQLDatabaseAsExpected(containers: Map<String, Container>, pullPolicy: DockerConfig.PullPolicy? = null) {
        assertContainerAsExpected(
            containers = containers,
            name = "sql-database",
            type = ContainerType.DATABASE,
            image = "postgres:12",
            environment = mapOf(
                "POSTGRES_DB" to "testdb",
                "POSTGRES_USER" to "user",
                "POSTGRES_PASSWORD" to "123",
            ),
            pullPolicy = pullPolicy ?: DockerConfig.PullPolicy.ALWAYS,
            hasHealthcheck = false,
            containerPorts = listOf(),
            databaseSystem = DatabaseManagementSystem.POSTGRESQL,
            database = "testdb",
            schema = "public",
            username = "user",
            password = "123",
            initializationScripts = mapOf("init_1" to "database/init_1.sql", "init_2" to "database/init_2.sql"),
        )
    }

    private fun assertNoSQLDatabaseAsExpected(containers: Map<String, Container>, pullPolicy: DockerConfig.PullPolicy? = null) {
        assertContainerAsExpected(
            containers = containers,
            name = "no-sql-database",
            type = ContainerType.DATABASE,
            image = "mongo:4.4.27",
            environment = mapOf(
                "MONGO_INITDB_DATABASE" to "testdb",
                "MONGO_INITDB_ROOT_USERNAME" to "user",
                "MONGO_INITDB_ROOT_PASSWORD" to "123",
            ),
            pullPolicy = pullPolicy ?: DockerConfig.PullPolicy.ALWAYS,
            hasHealthcheck = false,
            containerPorts = listOf(27017),
            databaseSystem = DatabaseManagementSystem.MONGO_DB,
            database = "testdb",
            username = "user",
            password = "123",
            initializationScripts = mapOf(),
        )
    }

    private fun assertContainerAsExpected(
        containers: Map<String, Container>,
        name: String,
        type: ContainerType,
        image: String?,
        environment: Map<String, String>?,
        pullPolicy: DockerConfig.PullPolicy,
        hasHealthcheck: Boolean,
        containerPorts: List<Int>,
        databaseSystem: DatabaseManagementSystem? = null,
        database: String? = null,
        schema: String? = null,
        username: String? = null,
        password: String? = null,
        initializationScripts: Map<String, String>? = null,
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
        assertEquals(pullPolicy, container.pullPolicy)
        assertEquals(hasHealthcheck, container.hasHealthcheck)
        assertEquals(containerPorts.size, container.ports.size)
        for (i in containerPorts.indices) {
            assertEquals(containerPorts[i], container.ports[i].internal)
        }
        if (container is DatabaseContainer) {
            assertEquals(databaseSystem, container.system)
            assertEquals(database, container.database)
            assertEquals(schema, container.schema)
            assertEquals(username, container.username)
            assertEquals(password, container.password)
            RecursiveComparison.assertEquals(initializationScripts, container.initializationScripts)
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

    private fun assertMockServerConfigAsExpected(expected: MockServerConfig, config: TestConfig) {
        RecursiveComparison.assertEquals(expected, config.settings.mockServers)
    }
}
