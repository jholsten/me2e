package org.jholsten.me2e.parsing

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.mockk.*
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.parsing.utils.SchemaValidator
import kotlin.test.*

internal class YamlParserTest {

    private val yamlMapper = mockk<YAMLMapper>()
    private val validator = mockk<SchemaValidator>()

    @BeforeTest
    fun beforeTest() {
        mockkObject(DeserializerFactory)
        every { DeserializerFactory.getYamlMapper() } returns yamlMapper

        mockkObject(FileUtils.Companion)
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Parsing YAML should validate and parse value`() {
        val value = "any-value"
        val parsed = Pair("A", "B")
        every { validator.validate(any()) } just runs
        every { yamlMapper.readValue(any<String>(), any<Class<*>>()) } returns parsed

        val parser = YamlParser(validator, Pair::class.java)
        val result = parser.parse(value)

        assertEquals(parsed, result)
        verify { validator.validate(value) }
        verify { yamlMapper.readValue(value, Pair::class.java) }
    }

    @Test
    fun `Parsing YAML from file should validate and parse value`() {
        val value = "any-value"
        val parsed = Pair("A", "B")
        every { FileUtils.readFileContentsFromResources(any()) } returns value
        every { validator.validate(any()) } just runs
        every { yamlMapper.readValue(any<String>(), any<Class<*>>()) } returns parsed

        val parser = YamlParser(validator, Pair::class.java)
        val result = parser.parseFile("file.yaml")

        assertEquals(parsed, result)
        verify { validator.validate(value) }
        verify { yamlMapper.readValue(value, Pair::class.java) }
        verify { FileUtils.readFileContentsFromResources("file.yaml") }
    }
}
