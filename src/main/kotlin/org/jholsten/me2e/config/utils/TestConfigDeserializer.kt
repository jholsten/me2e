package org.jholsten.me2e.config.utils

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
class TestConfigDeserializer : JsonDeserializer<TestConfig>() {
    private var mapper = DeserializerFactory.getObjectMapper()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestConfig {
        mapper = p.codec as ObjectMapper
        val node = p.readValueAsTree<ObjectNode>()

        val injectableValues = InjectableValues.Std()
        val requestConfig = deserializeRequestConfig(node.get("requests"))
        injectRequestConfig(injectableValues, requestConfig)

        val dockerConfig = deserializeDockerConfig(node.get("docker"))
        injectDockerConfig(injectableValues, dockerConfig)

        val mockServerConfig = deserializeMockServerConfig(node.get("mock-servers"))

        val environmentConfig = mapper.treeToValue(node.get("environment"), TestEnvironmentConfig::class.java)
        return TestConfig(
            docker = dockerConfig,
            requests = requestConfig,
            mockServers = mockServerConfig,
            environment = environmentConfig,
        )
    }

    private fun deserializeRequestConfig(requestConfigNode: JsonNode?): RequestConfig {
        if (requestConfigNode == null) {
            return RequestConfig()
        }
        return mapper.treeToValue(requestConfigNode, RequestConfig::class.java)
    }

    private fun injectRequestConfig(injectableValues: InjectableValues.Std, requestConfig: RequestConfig) {
        mapper.setInjectableValues(injectableValues.addValue("requestConfig", requestConfig))
    }

    private fun injectDockerConfig(injectableValues: InjectableValues.Std, dockerConfig: DockerConfig) {
        mapper.setInjectableValues(injectableValues.addValue("dockerConfig", dockerConfig))
    }

    private fun deserializeDockerConfig(dockerConfigNode: JsonNode?): DockerConfig {
        if (dockerConfigNode == null) {
            return DockerConfig()
        }
        return mapper.treeToValue(dockerConfigNode, DockerConfig::class.java)
    }

    private fun deserializeMockServerConfig(mockServerConfigNode: JsonNode?): MockServerConfig {
        if (mockServerConfigNode == null) {
            return MockServerConfig()
        }
        return mapper.treeToValue(mockServerConfigNode, MockServerConfig::class.java)
    }
}
