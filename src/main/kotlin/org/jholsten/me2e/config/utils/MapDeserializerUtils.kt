package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.parsing.utils.DeserializerFactory

internal class MapDeserializerUtils private constructor() {
    companion object {
        /**
         * Sets `name` field of the map's value objects to the key of the corresponding map entry.
         */
        @JvmStatic
        inline fun <reified T> setNameToKey(p: JsonParser): Map<String, T> {
            val result = mutableMapOf<String, T>()
            val node = p.readValueAsTree<ObjectNode>()
            for (entry in node.fields()) {
                val name = entry.key
                (entry.value as ObjectNode).put("name", name)
                result[name] = DeserializerFactory.getObjectMapper().readValue(entry.value.toString(), T::class.java)
            }

            return result
        }
    }
}
