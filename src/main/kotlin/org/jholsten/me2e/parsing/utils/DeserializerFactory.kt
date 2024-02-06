package org.jholsten.me2e.parsing.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Factory class for providing [ObjectMapper] instances as deserializers for JSON and YAML contents.
 */
internal class DeserializerFactory private constructor() {
    companion object {
        /**
         * Deserializer for contents in YAML or JSON format.
         */
        private val YAML_MAPPER: ObjectMapper = YAMLMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        /**
         * Deserializer for contents in JSON format.
         */
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        /**
         * Returns a copy of the [YAML_MAPPER] to be able to configure this instance without changing
         * the configuration for all usages. Can be used to deserialize contents in YAML or JSON format.
         */
        @JvmSynthetic
        fun getYamlMapper(): ObjectMapper {
            return YAML_MAPPER.copy()
        }

        /**
         * Returns a copy of the [OBJECT_MAPPER] to be able to configure this instance without changing
         * the configuration for all usages. Can be used to deserialize contents in JSON format.
         */
        @JvmSynthetic
        fun getObjectMapper(): ObjectMapper {
            return OBJECT_MAPPER.copy()
        }
    }
}
