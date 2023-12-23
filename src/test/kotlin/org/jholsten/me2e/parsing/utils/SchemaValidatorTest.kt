package org.jholsten.me2e.parsing.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.jholsten.me2e.parsing.exception.InvalidFormatException
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

internal class SchemaValidatorTest {

    companion object {
        private val YAML_MAPPER = YAMLMapper().registerModule(KotlinModule.Builder().build())
        private val JSON_MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())

        private val SCHEMA = """
            {
              "${"$"}id": "https://example.com/geographical-location.schema.json",
              "${"$"}schema": "http://json-schema.org/draft-07/schema#",
              "required": [ "latitude", "longitude" ],
              "type": "object",
              "properties": {
                "latitude": {
                    "type": "number",
                    "minimum": -90,
                    "maximum": 90
                },
                "longitude": {
                    "type": "number",
                    "minimum": -180,
                    "maximum": 180
                },
                "status": {
                    "type": "string",
                    "enum": ["PENDING", "ACCEPTED", "DECLINED"]
                },
                "id": {
                    "type": "string",
                    "pattern": "^[A-Z]{3}${"$"}"
                }
              }
            }
        """.trimIndent()
    }

    @BeforeTest
    fun beforeTest() {
        mockkObject(FileUtils.Companion)
        every { FileUtils.getResourceAsStream(any()) } returns SCHEMA.byteInputStream()
    }

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun `Validating valid YAML should not throw`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            latitude: 9.255876310462527
            longitude: 53.11505019602539
            status: PENDING
            id: ABC
        """.trimIndent()

        assertDoesNotThrow { validator.validate(value) }
    }

    @Test
    fun `Validating valid JSON should not throw`() {
        val validator = SchemaValidator("any-schema", JSON_MAPPER)
        val value = """
            {
                "latitude": 9.255876310462527,
                "longitude": 53.11505019602539,
                "status": "PENDING",
                "id": "ABC"
            }
        """.trimIndent()

        assertDoesNotThrow { validator.validate(value) }
    }

    @Test
    fun `Validating YAML with missing fields should fail`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            latitude: 9.255876310462527
        """.trimIndent()

        val e = assertFailsWith<ValidationException> { validator.validate(value) }

        assertEquals(1, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("longitude"))
    }

    @Test
    fun `Validating YAML with invalid type should fail`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            latitude: invalid-value
            longitude: 53.11505019602539
        """.trimIndent()

        val e = assertFailsWith<InvalidFormatException> { validator.validate(value) }

        assertNotNull(e.message)
        assertTrue(e.message!!.contains("latitude"))
    }

    @Test
    fun `Validating YAML with invalid enum value should fail`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            latitude: 9.255876310462527
            longitude: 53.11505019602539
            status: invalid-value
        """.trimIndent()

        val e = assertFailsWith<ValidationException> { validator.validate(value) }

        assertEquals(1, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("status"))
    }

    @Test
    fun `Validating YAML with violated constraints should fail`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            latitude: -100
            longitude: 53.11505019602539
        """.trimIndent()

        val e = assertFailsWith<ValidationException> { validator.validate(value) }

        assertEquals(1, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("latitude"))
    }

    @Test
    fun `Validating YAML with invalid regex value should fail`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            latitude: 9.255876310462527
            longitude: 53.11505019602539
            id: invalid-value
        """.trimIndent()

        val e = assertFailsWith<ValidationException> { validator.validate(value) }

        assertEquals(1, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("id"))
    }

    @Test
    fun `Validating string with invalid YAML format should fail`() {
        val validator = SchemaValidator("any-schema", YAML_MAPPER)
        val value = """
            invalid-yaml
        """.trimIndent()

        val e = assertFailsWith<InvalidFormatException> { validator.validate(value) }

        assertNotNull(e.message)
    }

    @Test
    fun `Validating string with invalid JSON format should fail`() {
        val validator = SchemaValidator("any-schema", JSON_MAPPER)
        val value = """
            invalid-json
        """.trimIndent()

        val e = assertFailsWith<InvalidFormatException> { validator.validate(value) }

        assertNotNull(e.message)
    }
}
