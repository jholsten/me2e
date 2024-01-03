package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jholsten.me2e.parsing.exception.ValidationException
import org.jholsten.util.assertDoesNotThrow
import kotlin.test.*

internal class ConfigSchemaValidatorIT {

    companion object {
        private val YAML_MAPPER = YAMLMapper().registerModule(KotlinModule.Builder().build())
        private val JSON_MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())
    }

    @Test
    fun `Validating valid YAML should not throw`() {
        val validator = ConfigSchemaValidator(YAML_MAPPER)
        val value = """
            environment:
              docker-compose: docker-compose-parsing-test.yml
        """.trimIndent()

        assertDoesNotThrow { validator.validate(value) }
    }

    @Test
    fun `Validating valid JSON should not throw`() {
        val validator = ConfigSchemaValidator(JSON_MAPPER)
        val value = """
            {
              "environment": {
                "docker-compose": "docker-compose-parsing-test.yml"
              }
            }
        """.trimIndent()

        assertDoesNotThrow { validator.validate(value) }
    }

    @Test
    fun `Validating YAML with missing fields should fail`() {
        val validator = ConfigSchemaValidator(YAML_MAPPER)
        val value = """
            other:
        """.trimIndent()

        val e = assertFailsWith<ValidationException> { validator.validate(value) }

        assertEquals(2, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("environment"))
        assertTrue(e.message!!.contains("other"))
    }

    @Test
    fun `Validating YAML with invalid regex value should fail`() {
        val validator = ConfigSchemaValidator(YAML_MAPPER)
        val value = """
            environment:
              docker-compose: docker-compose-parsing-test.yml
              mock-servers:
                payment-service:
                  hostname: example.com
                  stubs:
                    - invalid-file.txt
        """.trimIndent()

        val e = assertFailsWith<ValidationException> { validator.validate(value) }

        assertEquals(1, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("stubs"))
    }
}
