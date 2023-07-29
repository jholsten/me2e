package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.networknt.schema.JsonSchemaFactory
import io.mockk.*
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.utils.ConfigValidator
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrowsExactly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedInputStream
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class YamlConfigParserTest {

    private val yamlMapper = mockk<YAMLMapper>()

    private val contents = """
            containers:
              gateway-service:
                type: MICROSERVICE
                image: postgres:12
                environment:
                  DB_PASSWORD: 123
        """.trimIndent()

    @BeforeEach
    fun beforeEach() {
        mockkObject(DeserializerFactory.Companion)
        every { DeserializerFactory.getYamlMapper() } returns yamlMapper
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `Parsing valid YAML config should succeed`() {
        val config = testConfig()
        val filename = "any-file"
        mockDeserializationAndValidation(config)

        val result = YamlConfigParser().parseFile(filename)

        assertEquals(config, result)
        verify { anyConstructed<ConfigValidator>().validate(contents) }
        verify { FileUtils.readFileContentsFromResources(filename) }
    }

    @Test
    fun `Parsing invalid YAML should fail`() {
        val filename = "any-file"
        mockDeserializationAndValidation(testConfig())
        every { anyConstructed<ConfigValidator>().validate(any()) } throws InvalidFormatException("Some validation error")

        assertThrowsExactly(InvalidFormatException::class.java) { YamlConfigParser().parseFile(filename) }
    }

    @Test
    fun `Parsing YAML with MismatchedInputException on deserialization should fail`() {
        val filename = "any-file"
        mockDeserializationAndValidation(testConfig())
        val mismatchedInputException = MismatchedInputException.from(null as JsonParser?, "Some mismatched input")
        every { yamlMapper.readValue(contents, TestConfig::class.java) } throws mismatchedInputException

        val e = assertThrowsExactly(ValidationException::class.java) { YamlConfigParser().parseFile(filename) }

        assertNotNull(e.message)
        assertTrue(e.message!!.contains(mismatchedInputException.message!!))
    }

    @Test
    fun `Parsing YAML with Exception on deserialization should fail`() {
        val filename = "any-file"
        mockDeserializationAndValidation(testConfig())
        val exception = RuntimeException("Some error")
        every { yamlMapper.readValue(contents, TestConfig::class.java) } throws exception

        val e = assertThrowsExactly(ParseException::class.java) { YamlConfigParser().parseFile(filename) }

        assertNotNull(e.message)
        assertTrue(e.message!!.contains(exception.message!!))
    }

    private fun mockDeserializationAndValidation(config: TestConfig) {
        mockkObject(FileUtils.Companion)
        every { FileUtils.readFileContentsFromResources(any()) } returns contents

        mockkConstructor(ConfigValidator::class)
        every { anyConstructed<ConfigValidator>().validate(any()) } just runs

        // JsonSchemaFactory needs to be mocked since init-Block of SchemaValidator is executed even though it is mocked
        mockkConstructor(JsonSchemaFactory::class)
        every { anyConstructed<JsonSchemaFactory>().getSchema(any<InputStream>()) } returns mockk()

        every { yamlMapper.readValue(contents, TestConfig::class.java) } returns config
        every { yamlMapper.readTree(any<BufferedInputStream>()) } returns null
    }

    private fun testConfig(): TestConfig {
        return TestConfig(
            containers = mapOf(
                "gateway-service" to MicroserviceContainer(
                    name = "gateway-service",
                    image = "service:latest",
                ),
                "database" to Container(
                    name = "database",
                    type = ContainerType.DATABASE,
                    image = "postgres:12",
                )
            )
        )
    }
}
