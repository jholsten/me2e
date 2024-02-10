package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.*
import org.jholsten.me2e.parsing.utils.DeserializerFactory

/**
 * Custom deserializer that provides [RequestConfig] instance to be injected in other parsed instances.
 */
internal class TestConfigDeserializer : JsonDeserializer<TestConfig>() {
    private var mapper = DeserializerFactory.getObjectMapper()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestConfig {
        mapper = p.codec as ObjectMapper
        val node = p.readValueAsTree<ObjectNode>()

        val settings = deserializeSettings(node.get("settings"))
        val injectableValues = InjectableValues.Std()
        injectRequestConfig(injectableValues, settings.requests)
        injectDockerConfig(injectableValues, settings.docker)

        val environmentConfig = mapper.treeToValue(node.get("environment"), TestEnvironmentConfig::class.java)
        return TestConfig(
            settings = settings,
            environment = environmentConfig,
        )
    }

    private fun deserializeSettings(settingsNode: JsonNode?): TestSettings {
        if (settingsNode == null) {
            return TestSettings()
        }
        return mapper.treeToValue(settingsNode, TestSettings::class.java)
    }

    private fun injectRequestConfig(injectableValues: InjectableValues.Std, requestConfig: RequestConfig) {
        mapper.setInjectableValues(injectableValues.addValue("requestConfig", requestConfig))
    }

    private fun injectDockerConfig(injectableValues: InjectableValues.Std, dockerConfig: DockerConfig) {
        mapper.setInjectableValues(injectableValues.addValue("dockerConfig", dockerConfig))
    }
}
