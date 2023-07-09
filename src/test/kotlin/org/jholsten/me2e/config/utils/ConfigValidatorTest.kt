package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jholsten.me2e.config.exception.ValidationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConfigValidatorTest {
    
    companion object {
        private val YAML_MAPPER = YAMLMapper().registerModule(KotlinModule.Builder().build())
        private val JSON_MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())
    }
    
    @Test
    fun testValidateYaml() {
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
    fun testValidateJson() {
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
    fun testValidateWithMissingFields() {
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
    fun testValidateWithInvalidEnum() {
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
