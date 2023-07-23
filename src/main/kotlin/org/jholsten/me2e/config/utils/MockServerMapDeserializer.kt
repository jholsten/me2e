package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.jholsten.me2e.mock.MockServer

/**
 * Custom deserializer for map of `(mockServerName, mockServer)`.
 * Sets `name` field to `mockServerName` for each mock server.
 */
internal class MockServerMapDeserializer : JsonDeserializer<Map<String, MockServer>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, MockServer> {
        return MapDeserializerUtils.setNameToKey(p)
    }
}
