package org.jholsten.me2e.config.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.networknt.schema.JsonSchemaFactory
import io.mockk.*
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.parsing.exception.ParseException
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.config.utils.ConfigSchemaValidator
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import java.io.BufferedInputStream
import java.io.InputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class YamlConfigParserTest {

    private val yamlMapper = mockk<YAMLMapper>()

    private val contents = """
            environment:
              docker-compose: docker-compose-parsing-test.yml
        """.trimIndent()

    @BeforeTest
    fun beforeTest() {
        mockkObject(DeserializerFactory.Companion)
        every { DeserializerFactory.getYamlMapper() } returns yamlMapper
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Parsing valid YAML config should succeed`() {
        val filename = "any-file"
        val config = testConfig(filename)
        mockDeserializationAndValidation(config)

        val result = YamlConfigParser().parseFile(filename)

        assertEquals(config, result)
        verify { anyConstructed<ConfigSchemaValidator>().validate(contents) }
        verify { FileUtils.readFileContentsFromResources(filename) }
    }

    @Test
    fun `Parsing invalid YAML should fail`() {
        val filename = "any-file"
        mockDeserializationAndValidation(testConfig(filename))
        every { anyConstructed<ConfigSchemaValidator>().validate(any()) } throws InvalidFormatException("Some validation error")

        assertFailsWith<InvalidFormatException> { YamlConfigParser().parseFile(filename) }
    }

    @Test
    fun `Parsing YAML with MismatchedInputException on deserialization should fail`() {
        val filename = "any-file"
        mockDeserializationAndValidation(testConfig(filename))
        val mismatchedInputException = MismatchedInputException.from(null as JsonParser?, "Some mismatched input")
        every { yamlMapper.readValue(contents, TestConfig::class.java) } throws mismatchedInputException

        val e = assertFailsWith<ValidationException> { YamlConfigParser().parseFile(filename) }

        assertNotNull(e.message)
        assertTrue(e.message!!.contains(mismatchedInputException.message!!))
    }

    @Test
    fun `Parsing YAML with Exception on deserialization should fail`() {
        val filename = "any-file"
        mockDeserializationAndValidation(testConfig(filename))
        val exception = RuntimeException("Some error")
        every { yamlMapper.readValue(contents, TestConfig::class.java) } throws exception

        val e = assertFailsWith<ParseException> { YamlConfigParser().parseFile(filename) }

        assertNotNull(e.message)
        assertTrue(e.message!!.contains(exception.message!!))
    }

    private fun mockDeserializationAndValidation(config: TestConfig) {
        mockkObject(FileUtils.Companion)
        every { FileUtils.readFileContentsFromResources(any()) } returns contents

        mockkConstructor(ConfigSchemaValidator::class)
        every { anyConstructed<ConfigSchemaValidator>().validate(any()) } just runs

        // JsonSchemaFactory needs to be mocked since init-Block of SchemaValidator is executed even though it is mocked
        mockkConstructor(JsonSchemaFactory::class)
        every { anyConstructed<JsonSchemaFactory>().getSchema(any<InputStream>()) } returns mockk()

        every { yamlMapper.readValue(contents, TestConfig::class.java) } returns config
        every { yamlMapper.readTree(any<BufferedInputStream>()) } returns null
    }

    private fun testConfig(dockerComposeFile: String): TestConfig {
        return TestConfig(
            environment = TestEnvironmentConfig(
                dockerCompose = dockerComposeFile,
                containers = mapOf(
                    "api-gateway" to MicroserviceContainer(
                        name = "api-gateway",
                        image = "service:latest",
                        requestConfig = RequestConfig()
                    ),
                ),
            ),
        )
    }
}
