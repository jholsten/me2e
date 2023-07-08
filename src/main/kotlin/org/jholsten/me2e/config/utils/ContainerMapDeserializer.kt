package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jholsten.me2e.container.Container

/**
 * Custom deserializer for map of `(containerName, container)`.
 * Sets `name` field to `containerName` for each container.
 */
class ContainerMapDeserializer: JsonDeserializer<Map<String, Container>>() {
    companion object {
        val OBJECT_MAPPER = ObjectMapper().registerModule(KotlinModule.Builder().build())
    }
    
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, Container> {
        val containers = mutableMapOf<String, Container>()
        val node = p.readValueAsTree<ObjectNode>()
        for (entry in node.fields()) {
            val containerName = entry.key
            (entry.value as ObjectNode).put("name", containerName)
            containers[containerName] = OBJECT_MAPPER.readValue(entry.value.toString(), Container::class.java)
        }
        
        return containers
    }
}
