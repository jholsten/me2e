package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jholsten.me2e.parsing.exception.ValidationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ConfigValidatorIT {

    companion object {
        private val YAML_MAPPER = YAMLMapper().registerModule(KotlinModule.Builder().build())
        private val JSON_MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())
    }

    @Test
    fun `Validating valid YAML should not throw`() {
        val value = """
            containers:
              gateway-service:
                type: MICROSERVICE
                image: postgres:12
                environment:
                  DB_PASSWORD: 123
        """.trimIndent()

        assertDoesNotThrow { ConfigValidator.validate(value, YAML_MAPPER) }
    }

    @Test
    fun `Validating valid JSON should not throw`() {
        val value = """
            {
              "containers": {
                "gateway-service": {
                  "type": "MICROSERVICE",
                  "image": "postgres:12",
                  "environment": {
                    "DB_PASSWORD": "123"
                  }
                }
              }
            }
        """.trimIndent()

        assertDoesNotThrow { ConfigValidator.validate(value, JSON_MAPPER) }
    }

    @Test
    fun `Validating YAML with missing fields should fail`() {
        val value = """
            containers:
              gateway-service:
                environment:
                  DB_PASSWORD: 123
        """.trimIndent()

        val e = assertThrowsExactly(ValidationException::class.java) { ConfigValidator.validate(value, YAML_MAPPER) }

        assertEquals(2, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("type"))
        assertTrue(e.message!!.contains("image"))
    }

    @Test
    fun `Validating YAML with invalid enum value should fail`() {
        val value = """
            containers:
              gateway-service:
                type: invalid
                image: postgres:12
                environment:
                  DB_PASSWORD: 123
        """.trimIndent()

        val e = assertThrowsExactly(ValidationException::class.java) { ConfigValidator.validate(value, YAML_MAPPER) }

        assertEquals(1, e.validationErrors.size)
        assertNotNull(e.message)
        assertTrue(e.message!!.contains("type"))
    }
}
