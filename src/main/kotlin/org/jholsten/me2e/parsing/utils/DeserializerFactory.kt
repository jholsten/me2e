package org.jholsten.me2e.parsing.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

internal class DeserializerFactory private constructor() {
    companion object {
        @JvmStatic
        private val YAML_MAPPER: ObjectMapper = YAMLMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        @JvmStatic
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        @JvmStatic
        fun getYamlMapper(): ObjectMapper {
            return YAML_MAPPER.copy()
        }

        @JvmStatic
        fun getObjectMapper(): ObjectMapper {
            return OBJECT_MAPPER.copy()
        }
    }
}
