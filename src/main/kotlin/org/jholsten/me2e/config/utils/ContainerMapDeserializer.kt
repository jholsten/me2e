package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.jholsten.me2e.container.Container

/**
 * Custom deserializer for map of `(containerName, container)`.
 * Sets `name` field to `containerName` for each container.
 */
internal class ContainerMapDeserializer : JsonDeserializer<Map<String, Container>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, Container> {
        return MapDeserializerUtils.setNameToKey(p)
    }
}
