package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.*
import org.jholsten.me2e.config.model.DockerConfig
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
                healthcheck:
                  test: curl --fail http://localhost:1234/health || exit 1
                labels:
                  "org.jholsten.me2e.container-type": "MICROSERVICE"
                  "org.jholsten.me2e.pull-policy": "ALWAYS"

            auth-server:
                image: auth-server:1.3.0
                labels:
                  "org.jholsten.me2e.container-type": "MICROSERVICE"
                  "org.jholsten.me2e.url": "http://auth-server"

            database:
                image: postgres:12
                environment:
                  - DB_PASSWORD=123
                  - DB_USER=user
                labels:
                  - "org.jholsten.me2e.container-type=DATABASE"
                  - "org.jholsten.me2e.database.system=POSTGRESQL"
    """.trimIndent()

    private val mockedMapper = spyk<ObjectMapper>()
    private val yamlMapper = YAMLMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val mockedDeserializationContext = mockDeserializationContext()

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
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("api-gateway", "auth-server", "database"), config.containers)
        assertKeysAsExpected(listOf("mock-server"), config.mockServers)

        verify { mockedMapper.treeToValue(expectedApiGateway(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedAuthServer(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedDatabase(), Container::class.java) }
        verify { mockedMapper.treeToValue(expectedMockServer(), MockServer::class.java) }
    }

    @Test
    fun `Deserializing environment config without Mock Servers should succeed`() {
        val contents = "docker-compose: docker-compose-parsing-test.yml"
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

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
                    healthcheck:
                      test: curl --fail http://localhost:1234/health || exit 1
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("api-gateway"), config.containers)
        assertKeysAsExpected(listOf("mock-server"), config.mockServers)

        val expectedService = expectedApiGateway()
            .put("type", "MISC")
            .put("predefinedUrl", null as String?)
            .put("pullPolicy", "MISSING")
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
                    healthcheck:
                      test: curl --fail http://localhost:1234/health || exit 1
                    labels:
                      - "org.jholsten.me2e.container-type: MICROSERVICE"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("api-gateway"), config.containers)
        assertKeysAsExpected(listOf("mock-server"), config.mockServers)

        val expectedService = expectedApiGateway()
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("DB_USER", "user")
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
            )
            .put("type", "MISC")
            .put("pullPolicy", "MISSING")
        verify { mockedMapper.treeToValue(expectedService, Container::class.java) }
        verify { mockedMapper.treeToValue(expectedMockServer(), MockServer::class.java) }
    }

    @Test
    fun `Deserializing database properties with labels should succeed`() {
        val dockerComposeContents = """
            services:
                database:
                    image: postgres:12
                    labels:
                      - "org.jholsten.me2e.container-type=DATABASE"
                      - "org.jholsten.me2e.database.system=POSTGRESQL"
                      - "org.jholsten.me2e.database.name=testdb"
                      - "org.jholsten.me2e.database.schema=public"
                      - "org.jholsten.me2e.database.username=user"
                      - "org.jholsten.me2e.database.password=123"
                      - "org.jholsten.me2e.database.init-script.init_1=database/init_1.sql"
                      - "org.jholsten.me2e.database.init-script.init_2=database/init_2.sql"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("database"), config.containers)

        val expectedDatabase = expectedDatabase()
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database.system", "POSTGRESQL")
                    .put("org.jholsten.me2e.database.name", "testdb")
                    .put("org.jholsten.me2e.database.schema", "public")
                    .put("org.jholsten.me2e.database.username", "user")
                    .put("org.jholsten.me2e.database.password", "123")
                    .put("org.jholsten.me2e.database.init-script.init_1", "database/init_1.sql")
                    .put("org.jholsten.me2e.database.init-script.init_2", "database/init_2.sql")
            )
            .put("pullPolicy", "MISSING")
            .put("system", "POSTGRESQL")
            .put("schema", "public")
            .put("database", "testdb")
            .put("username", "user")
            .put("password", "123")
            .set<ObjectNode>(
                "initializationScripts", JsonNodeFactory.instance.objectNode()
                    .put("init_1", "database/init_1.sql")
                    .put("init_2", "database/init_2.sql")
            )
        expectedDatabase.remove("environment")

        verify { mockedMapper.treeToValue(expectedDatabase, Container::class.java) }
    }

    @Test
    fun `Deserializing database properties without labels should succeed for PostgreSQL`() {
        val dockerComposeContents = """
            services:
                database:
                    image: postgres:12
                    environment:
                        POSTGRES_DB: testdb
                        POSTGRES_USER: user
                        POSTGRES_PASSWORD: 123
                    labels:
                      - "org.jholsten.me2e.container-type=DATABASE"
                      - "org.jholsten.me2e.database.system=POSTGRESQL"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("database"), config.containers)

        val expectedDatabase = expectedDatabase()
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("POSTGRES_DB", "testdb")
                    .put("POSTGRES_USER", "user")
                    .put("POSTGRES_PASSWORD", 123)
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database.system", "POSTGRESQL")
            )
            .put("pullPolicy", "MISSING")
            .put("system", "POSTGRESQL")
            .put("schema", null as String?)
            .put("database", "testdb")
            .put("username", "user")
            .put("password", "123")

        verify { mockedMapper.treeToValue(expectedDatabase, Container::class.java) }
    }

    @Test
    fun `Deserializing database properties without labels should succeed for MySQL`() {
        val dockerComposeContents = """
            services:
                database:
                    image: mysql:8.0
                    environment:
                        MYSQL_DATABASE: testdb
                        MYSQL_USER: user
                        MYSQL_PASSWORD: 123
                    labels:
                      - "org.jholsten.me2e.container-type=DATABASE"
                      - "org.jholsten.me2e.database.system=MY_SQL"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("database"), config.containers)

        val expectedDatabase = expectedDatabase()
            .put("image", "mysql:8.0")
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("MYSQL_DATABASE", "testdb")
                    .put("MYSQL_USER", "user")
                    .put("MYSQL_PASSWORD", 123)
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database.system", "MY_SQL")
            )
            .put("pullPolicy", "MISSING")
            .put("system", "MY_SQL")
            .put("schema", null as String?)
            .put("database", "testdb")
            .put("username", "user")
            .put("password", "123")

        verify { mockedMapper.treeToValue(expectedDatabase, Container::class.java) }
    }

    @Test
    fun `Deserializing database properties without labels should succeed for MariaDB`() {
        val dockerComposeContents = """
            services:
                database:
                    image: mariadb:11.2.2
                    environment:
                        MYSQL_DATABASE: testdb
                        MYSQL_USER: user
                        MYSQL_PASSWORD: 123
                    labels:
                      - "org.jholsten.me2e.container-type=DATABASE"
                      - "org.jholsten.me2e.database.system=MARIA_DB"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("database"), config.containers)

        val expectedDatabase = expectedDatabase()
            .put("image", "mariadb:11.2.2")
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("MYSQL_DATABASE", "testdb")
                    .put("MYSQL_USER", "user")
                    .put("MYSQL_PASSWORD", 123)
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database.system", "MARIA_DB")
            )
            .put("pullPolicy", "MISSING")
            .put("system", "MARIA_DB")
            .put("schema", null as String?)
            .put("database", "testdb")
            .put("username", "user")
            .put("password", "123")

        verify { mockedMapper.treeToValue(expectedDatabase, Container::class.java) }
    }

    @Test
    fun `Deserializing database properties without labels should succeed for MongoDB`() {
        val dockerComposeContents = """
            services:
                database:
                    image: mongo:4.4.27
                    environment:
                        MONGO_INITDB_DATABASE: testdb
                        MONGO_INITDB_ROOT_USERNAME: user
                        MONGO_INITDB_ROOT_PASSWORD: 123
                    labels:
                      - "org.jholsten.me2e.container-type=DATABASE"
                      - "org.jholsten.me2e.database.system=MONGO_DB"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("database"), config.containers)

        val expectedDatabase = expectedDatabase()
            .put("image", "mongo:4.4.27")
            .set<ObjectNode>(
                "environment", JsonNodeFactory.instance.objectNode()
                    .put("MONGO_INITDB_DATABASE", "testdb")
                    .put("MONGO_INITDB_ROOT_USERNAME", "user")
                    .put("MONGO_INITDB_ROOT_PASSWORD", 123)
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database.system", "MONGO_DB")
            )
            .put("pullPolicy", "MISSING")
            .put("system", "MONGO_DB")
            .put("schema", null as String?)
            .put("database", "testdb")
            .put("username", "user")
            .put("password", "123")

        verify { mockedMapper.treeToValue(expectedDatabase, Container::class.java) }
    }

    @Test
    fun `Deserializing database properties without labels should succeed for unknown system`() {
        val dockerComposeContents = """
            services:
                database:
                    image: unknown-db:latest
                    labels:
                      - "org.jholsten.me2e.container-type=DATABASE"
                      - "org.jholsten.me2e.database.system=OTHER"
        """.trimIndent()
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        val config = TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)

        assertKeysAsExpected(listOf("database"), config.containers)

        val expectedDatabase = expectedDatabase()
            .put("image", "unknown-db:latest")
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "DATABASE")
                    .put("org.jholsten.me2e.database.system", "OTHER")
            )
            .put("pullPolicy", "MISSING")
            .put("system", "OTHER")
        expectedDatabase.remove("environment")

        verify { mockedMapper.treeToValue(expectedDatabase, Container::class.java) }
    }

    @Test
    fun `Deserializing environment config with invalid Docker-Compose should fail`() {
        val dockerComposeContents = "something: else"
        mockReadingDockerCompose(dockerComposeContents)

        val parser = prepareParser(contents)
        assertFailsWith<IllegalArgumentException> {
            TestEnvironmentConfigDeserializer().deserialize(parser, mockedDeserializationContext)
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

    private fun mockDeserializationContext(dockerConfig: DockerConfig = DockerConfig()): DeserializationContext {
        val mockedContext = mockk<DeserializationContext>()
        every { mockedContext.findInjectableValue(any(), any(), any()) } returns dockerConfig
        return mockedContext
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
                "healthcheck", JsonNodeFactory.instance.objectNode()
                    .put("test", "curl --fail http://localhost:1234/health || exit 1")
            )
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "MICROSERVICE")
                    .put("org.jholsten.me2e.pull-policy", "ALWAYS")
            )
            .put("name", "api-gateway")
            .put("type", "MICROSERVICE")
            .put("predefinedUrl", null as String?)
            .put("pullPolicy", "ALWAYS")
            .put("hasHealthcheck", true)
    }

    private fun expectedAuthServer(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
            .put("image", "auth-server:1.3.0")
            .set<ObjectNode>(
                "labels", JsonNodeFactory.instance.objectNode()
                    .put("org.jholsten.me2e.container-type", "MICROSERVICE")
                    .put("org.jholsten.me2e.url", "http://auth-server")
            )
            .put("name", "auth-server")
            .put("type", "MICROSERVICE")
            .put("predefinedUrl", "http://auth-server")
            .put("pullPolicy", "MISSING")
            .put("hasHealthcheck", false)
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
                    .put("org.jholsten.me2e.database.system", "POSTGRESQL")
            )
            .put("name", "database")
            .put("type", "DATABASE")
            .put("predefinedUrl", null as String?)
            .put("pullPolicy", "MISSING")
            .put("hasHealthcheck", false)
            .put("system", "POSTGRESQL")
            .put("schema", null as String?)
            .put("database", null as String?)
            .put("username", null as String?)
            .put("password", null as String?)
            .set("initializationScripts", JsonNodeFactory.instance.objectNode())
    }

    private fun expectedMockServer(): ObjectNode {
        return JsonNodeFactory.instance.objectNode()
            .put("hostname", "mock.example.com")
            .set<ObjectNode>("stubs", JsonNodeFactory.instance.arrayNode().add("stub.json"))
            .put("name", "mock-server")
    }
}
