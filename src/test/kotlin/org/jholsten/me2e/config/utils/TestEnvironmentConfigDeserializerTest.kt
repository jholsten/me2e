package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import kotlin.test.*

class TestEnvironmentConfigDeserializerTest {

    private val contents = """
        docker-compose: docker-compose-parsing-test.yml
        mock-servers:
            mock-server:
                hostname: mock.example.com
                stubs:
                    - stub.json
    """.trimIndent()

    private val dockerComposeContents = """
        services:
            api-gateway:
                image: api-gateway-service:latest
                ports:
                  - 1234
                  - 1235:80
                environment:
                  DB_PASSWORD: 123
                  DB_USER: user
                labels:
                  "org.jholsten.me2e.container-type": "MICROSERVICE"
                  "org.jholsten.me2e.is-public": "true"

            auth-server:
                image: auth-server:1.3.0
                labels:
                  "org.jholsten.me2e.container-type": "MICROSERVICE"
                  "org.jholsten.me2e.is-public": "true"

            database:
                image: postgres:12
                environment:
                  - DB_PASSWORD=123
                  - DB_USER=user
                labels:
                  - "org.jholsten.me2e.container-type=DATABASE"
                  - "org.jholsten.me2e.database-type=POSTGRESQL"
    """.trimIndent()

    private val mockedMapper = spyk<ObjectMapper>()
    private val yamlMapper = YAMLMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @BeforeTest
    fun beforeTest() {
        mockkObject(DeserializerFactory.Companion)
        every { DeserializerFactory.getObjectMapper() } returns mockedMapper
        every { DeserializerFactory.getYamlMapper() } returns yamlMapper
        initializeMockedMapper()
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Deserializing environment config should succeed`() {
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, yamlMapper.deserializationContext)

        assertKeysAsExpected(listOf("api-gateway", "auth-server", "database"), config.containers)
        assertKeysAsExpected(listOf("mock-server"), config.mockServers)

        verify { mockedMapper.treeToValue(expectedApiGateway(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedAuthServer(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedDatabase(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedMockServer(), MockServer::class.java) }
    }

    @Test
    fun `Deserializing environment config without mock servers should succeed`() {
        val contents = "docker-compose: docker-compose-parsing-test.yml"
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, yamlMapper.deserializationContext)

        assertKeysAsExpected(listOf("api-gateway", "auth-server", "database"), config.containers)

        verify { mockedMapper.treeToValue(expectedApiGateway(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedAuthServer(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedDatabase(), Container::class.java) }
    }

    @Test
    fun `Deserializing environment config without labels should succeed`() {
        val dockerComposeContents = """
            services:
                api-gateway:
                    image: api-gateway-service:latest
                    ports:
                      - 1234
                      - 1235:80
                    environment:
                      DB_PASSWORD: 123
                      DB_USER: user
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, yamlMapper.deserializationContext)

        assertKeysAsExpected(listOf("api-gateway"), config.containers)
        assertKeysAsExpected(listOf("mock-server"), config.mockServers)

        val expectedService = expectedApiGateway()
            .put("type", null as String?)
            .put("system", null as String?)
            .put("public", null as String?)
        expectedService.remove("labels")
        verify { mockedMapper.treeToValue(expectedService, Container::class.java) }
        verify { mockedMapper.treeToValue(expectedMockServer(), MockServer::class.java) }
    }

    @Test
    fun `Deserializing environment config with invalid key value pairs should succeed`() {
        val dockerComposeContents = """
            services:
                api-gateway:
                    image: api-gateway-service:latest
                    ports:
                      - 1234
                      - 1235:80
                    environment:
                      - "DB_PASSWORD: 123"
                      - "DB_USER=user"
                    labels:
                      - "org.jholsten.me2e.container-type: MICROSERVICE"
                      - "org.jholsten.me2e.is-public=true"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, yamlMapper.deserializationContext)

        assertKeysAsExpected(listOf("api-gateway"), config.containers)
        assertKeysAsExpected(listOf("mock-server"), config.mockServers)

        val expectedService = expectedApiGateway()
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("DB_USER", "user")
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.is-public", "true")
            )
            .put("type", null as String?)
        verify { mockedMapper.treeToValue(expectedService, Container::class.java) }
        verify { mockedMapper.treeToValue(expectedMockServer(), MockServer::class.java) }
    }

    @Test
    fun `Deserializing environment config with invalid Docker-Compose should fail`() {
        val dockerComposeContents = "something: else"
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        assertFailsWith<IllegalArgumentException> {
            TestEnvironmentConfigDeserializer().deserialize(parser, yamlMapper.deserializationContext)
        }
    }

    private fun mockReadingDockerCompose(dockerComposeContents: String) {
        mockkObject(FileUtils)
        every { FileUtils.readFileContentsFromResources(any()) } returns dockerComposeContents
    }

    private fun initializeMockedMapper() {
        every { mockedMapper.treeToValue(any<JsonNode>(), Container::class.java) } returns mockk<Container>()
        every { mockedMapper.treeToValue(any<JsonNode>(), MockServer::class.java) } returns mockk<MockServer>()
    }

    /**
     * Prepares JSON parser by reading the given [value] as tokens.
     * Sets `codec` to the [mockedMapper] in order to mock the deserialization of [MockServer] and [Container] instances.
     */
    private fun prepareParser(value: String): JsonParser {
        val parser = yamlMapper.factory.createParser(value)
        parser.codec = mockedMapper
        return parser
    }

    private fun assertKeysAsExpected(expectedKeys: List<String>, actual: Map<String, Any>) {
        assertEquals(expectedKeys.size, actual.size)
        for (name in expectedKeys) {
            assertTrue(actual.containsKey(name))
        }
    }

    private fun expectedApiGateway(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
            .put("image", "api-gateway-service:latest")
            .set<ObjectNode>("ports", JsonNodeFactory.instance.arrayNode().add(1234).add("1235:80"))
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("DB_PASSWORD", 123)
                    .put("DB_USER", "user")
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "MICROSERVICE")
                    .put("org.jholsten.me2e.is-public", "true")
            )
            .put("name", "api-gateway")
            .put("type", "MICROSERVICE")
            .put("system", null as String?)
            .put("public", "true")
    }

    private fun expectedAuthServer(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
            .put("image", "auth-server:1.3.0")
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "MICROSERVICE")
                    .put("org.jholsten.me2e.is-public", "true")
            )
            .put("name", "auth-server")
            .put("type", "MICROSERVICE")
            .put("system", null as String?)
            .put("public", "true")
    }

    private fun expectedDatabase(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
            .put("image", "postgres:12")
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("DB_PASSWORD", "123")
                    .put("DB_USER", "user")
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database-type", "POSTGRESQL")
            )
            .put("name", "database")
            .put("type", "DATABASE")
            .put("system", "POSTGRESQL")
            .put("public", null as String?)
    }

    private fun expectedMockServer(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
            .put("hostname", "mock.example.com")
            .set<ObjectNode>("stubs", JsonNodeFactory.instance.arrayNode().add("stub.json"))
            .put("name", "mock-server")
    }
}
