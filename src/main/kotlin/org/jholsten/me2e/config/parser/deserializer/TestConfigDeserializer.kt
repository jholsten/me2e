package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.*

/**
 * Custom deserializer for parsing the test configuration defined in the config file to an instance
 * of [TestConfig]. Includes providing [RequestConfig] and [DockerConfig] instance to be injected in
 * other parsed instances.
 */
internal class TestConfigDeserializer : JsonDeserializer<TestConfig>() {
    companion object {
        /**
         * Name of the injectable field which contains the deserialized [RequestConfig]
         * defined in the [TestConfig.settings]. Can be injected in other objects using
         * [com.fasterxml.jackson.annotation.JacksonInject].
         */
        @JvmSynthetic
        internal const val INJECTABLE_REQUEST_CONFIG_FIELD_NAME = "requestConfig"

        /**
         * Name of the injectable field which contains the deserialized [DockerConfig]
         * defined in the [TestConfig.settings]. Can be injected in other objects using
         * [com.fasterxml.jackson.annotation.JacksonInject].
         */
        @JvmSynthetic
        internal const val INJECTABLE_DOCKER_CONFIG_FIELD_NAME = "dockerConfig"
    }

    /**
     * Object mapper to use for deserializing the sections of the configuration.
     */
    private lateinit var mapper: ObjectMapper

    /**
     * Deserializes contents of a ME2E test config file to an instance of [TestConfig].
     * @return Deserialized test configuration.
     */
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

    /**
     * Deserializes settings defined in node [settingsNode] to an instance of [TestSettings]
     * using the object mapper of this deserializer. Since the settings are optional, this
     * node may not be present. In this case, the default settings are returned.
     * @return Deserialized test settings.
     */
    private fun deserializeSettings(settingsNode: JsonNode?): TestSettings {
        if (settingsNode == null) {
            return TestSettings()
        }
        return mapper.treeToValue(settingsNode, TestSettings::class.java)
    }

    /**
     * Sets injectable field with name [INJECTABLE_REQUEST_CONFIG_FIELD_NAME] to value of [requestConfig] to be able
     * to inject this value in the [org.jholsten.me2e.container.microservice.MicroserviceContainer.requestConfig].
     */
    private fun injectRequestConfig(injectableValues: InjectableValues.Std, requestConfig: RequestConfig) {
        mapper.setInjectableValues(injectableValues.addValue(INJECTABLE_REQUEST_CONFIG_FIELD_NAME, requestConfig))
    }

    /**
     * Sets injectable field with name [INJECTABLE_DOCKER_CONFIG_FIELD_NAME] to value of [dockerConfig] to be able
     * to inject this value in the [TestEnvironmentConfigDeserializer].
     */
    private fun injectDockerConfig(injectableValues: InjectableValues.Std, dockerConfig: DockerConfig) {
        mapper.setInjectableValues(injectableValues.addValue(INJECTABLE_DOCKER_CONFIG_FIELD_NAME, dockerConfig))
    }
}
