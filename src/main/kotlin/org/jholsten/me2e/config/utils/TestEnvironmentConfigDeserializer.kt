package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger

/**
 * Custom deserializer for deserializing test environment configuration.
 * Deserializes services in `docker-compose` file to [Container] instances.
 * Also, it sets the name of the specified mock servers to the corresponding key.
 */
class TestEnvironmentConfigDeserializer : JsonDeserializer<TestEnvironmentConfig>() {
    companion object {
        /**
         * Label key for specifying the container type of a service
         */
        private const val CONTAINER_TYPE_KEY = "org.jholsten.me2e.container-type"

        /**
         * Label key for specifying the URL of a microservice.
         */
        private const val URL_KEY = "org.jholsten.me2e.url"

        /**
         * Label key for specifying the database type (i.e. Database Management System) of a service.
         * Only applicable to services of type `DATABASE`.
         */
        private const val DATABASE_TYPE_KEY = "org.jholsten.me2e.database-type"
    }

    private val logger = logger(this)
    private var mapper = DeserializerFactory.getObjectMapper()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestEnvironmentConfig {
        mapper = p.codec as ObjectMapper
        val node = p.readValueAsTree<ObjectNode>()
        val fields = node.fields().asSequence().map { it.key to it.value }.toMap()
        return TestEnvironmentConfig(
            containers = deserializeContainers(fields["docker-compose"]!!.textValue()),
            mockServers = fields["mock-servers"]?.let { deserializeMockServers(it) } ?: mapOf(),
        )
    }

    /**
     * Deserializes mock server instances to Map of `(mockServerName, mockServer)`.
     * Sets `name` field to `mockServerName` for each mock server.
     */
    private fun deserializeMockServers(mockServersNode: JsonNode): Map<String, MockServer> {
        val result = mutableMapOf<String, MockServer>()
        for (entry in mockServersNode.fields()) {
            val name = entry.key
            (entry.value as ObjectNode).put("name", name)
            result[name] = mapper.treeToValue(entry.value, MockServer::class.java)
        }

        return result
    }

    /**
     * Deserializes services from the given Docker-Compose file to Map of `(containerName, Container)`.
     * Reads configuration properties from labels.
     */
    private fun deserializeContainers(dockerComposeFile: String): Map<String, Container> {
        val dockerComposeContent = FileUtils.readFileContentsFromResources(dockerComposeFile)
        val dockerCompose = DeserializerFactory.getYamlMapper().readTree(dockerComposeContent)
        val serviceNode = dockerCompose.get("services")
        require(serviceNode != null) { "Docker-Compose needs to have services defined." }
        val services = serviceNode.fields()
        return deserializeContainers(services)
    }

    private fun deserializeContainers(services: Iterator<MutableMap.MutableEntry<String, JsonNode>>): Map<String, Container> {
        val result = mutableMapOf<String, Container>()
        for (entry in services) {
            val node = entry.value as ObjectNode
            convertListToMap(node, "labels")
            convertListToMap(node, "environment")
            val labels = getImageLabels(node.get("labels"))
            node.put("name", entry.key)
            node.put("type", labels[CONTAINER_TYPE_KEY])
            node.put("system", labels[DATABASE_TYPE_KEY])
            node.put("url", labels[URL_KEY])
            node.put("hasHealthcheck", node.has("healthcheck"))
            result[entry.key] = mapper.treeToValue(node, Container::class.java)
        }
        return result
    }

    private fun getImageLabels(labelsNode: JsonNode?): Map<String, String?> {
        return mapOf(
            CONTAINER_TYPE_KEY to labelsNode?.get(CONTAINER_TYPE_KEY)?.textValue(),
            URL_KEY to labelsNode?.get(URL_KEY)?.textValue(),
            DATABASE_TYPE_KEY to labelsNode?.get(DATABASE_TYPE_KEY)?.textValue(),
        )
    }

    /**
     * For some entries in the Docker-Compose file (e.g. `environment` and `labels`), it is possible to define the key-value-pairs
     * in two different ways (see [Docker-Documentation](https://docs.docker.com/compose/compose-file/compose-file-v3/#environment)):
     *
     * **Option 1**:
     * ```
     * environment:
     *   RACK_ENV: development
     *   SESSION_SECRET:
     * ```
     * **Option 2**:
     * ```
     * environment:
     *   - RACK_ENV=development
     *   - SESSION_SECRET
     * ```
     * Since we need the entry in the format of Option 1, we need to convert entries given in Option-2-Format.
     */
    private fun convertListToMap(serviceNode: ObjectNode, key: String) {
        if (serviceNode[key] == null || !serviceNode[key].isArray) {
            return
        }

        val result = mutableMapOf<String, String>()
        for (entry in serviceNode[key].elements()) {
            val keyValuePair = entry.textValue().split("=")
            if (keyValuePair.size != 2) {
                logger.warn("Ignoring entry ${entry.textValue()} in $key since the entry is not in the required format.")
                continue
            }
            result[keyValuePair[0]] = keyValuePair[1]
        }
        serviceNode.replace(key, mapper.valueToTree(result))
    }
}
