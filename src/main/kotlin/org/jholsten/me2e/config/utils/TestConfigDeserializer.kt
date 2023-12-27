package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.RequestConfig
import org.jholsten.me2e.config.model.TestConfig
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Custom deserializer that provides [RequestConfig] instance to be injected in other parsed instances.
 */
class TestConfigDeserializer : JsonDeserializer<TestConfig>() {
    private var mapper = DeserializerFactory.getObjectMapper()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestConfig {
        mapper = p.codec as ObjectMapper
        val node = p.readValueAsTree<ObjectNode>()

        val requestConfig = deserializeRequestConfig(node.get("requests"))
        injectRequestConfig(requestConfig)

        val environmentConfig = mapper.treeToValue(node.get("environment"), TestEnvironmentConfig::class.java)
        return TestConfig(
            requests = requestConfig,
            environment = environmentConfig,
        )
    }

    private fun deserializeRequestConfig(requestConfigNode: JsonNode?): RequestConfig {
        if (requestConfigNode == null) {
            return RequestConfig()
        }
        return mapper.treeToValue(requestConfigNode, RequestConfig::class.java)
    }

    private fun injectRequestConfig(requestConfig: RequestConfig) {
        mapper.setInjectableValues(InjectableValues.Std().addValue("requestConfig", requestConfig))
    }
}
