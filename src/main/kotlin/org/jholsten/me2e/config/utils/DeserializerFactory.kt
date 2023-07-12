package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

internal class DeserializerFactory {
    companion object {
        private val YAML_MAPPER: ObjectMapper = YAMLMapper().registerModule(KotlinModule.Builder().build())
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        
        fun getYamlMapper(): ObjectMapper {
            return YAML_MAPPER
        }
        
        fun getObjectMapper(): ObjectMapper {
            return OBJECT_MAPPER
        }
    }
}
