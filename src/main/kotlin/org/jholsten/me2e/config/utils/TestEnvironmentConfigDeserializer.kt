package org.jholsten.me2e.config.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger

/**
 * Custom deserializer for deserializing test environment configuration.
 * Deserializes services in `docker-compose` file to [Container] instances.
 * Also, it sets the name of the specified Mock Servers to the corresponding key.
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
         * Only applicable to services of type [ContainerType.DATABASE].
         */
        private const val DATABASE_SYSTEM_KEY = "org.jholsten.me2e.database.system"

        /**
         * Label key for specifying the name of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         */
        private const val DATABASE_NAME_KEY = "org.jholsten.me2e.database.name"

        /**
         * Label key for specifying the schema of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE] and systems of type [DatabaseManagementSystem.POSTGRESQL].
         */
        private const val DATABASE_SCHEMA_KEY = "org.jholsten.me2e.database.schema"

        /**
         * Label key for specifying the username of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         */
        private const val DATABASE_USERNAME_KEY = "org.jholsten.me2e.database.username"

        /**
         * Label key for specifying the password of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         */
        private const val DATABASE_PASSWORD_KEY = "org.jholsten.me2e.database.password"

        /**
         * Regex to match label keys for specifying initialization scripts for the database.
         * Only applicable to services of type [ContainerType.DATABASE].
         */
        private val DATABASE_INIT_SCRIPTS_KEY_REGEX = Regex("org\\.jholsten\\.me2e\\.database\\.init-script\\.(.*)")

        /**
         * Label key for specifying the pull policy for this container.
         */
        private const val PULL_POLICY_KEY = "org.jholsten.me2e.pull-policy"
    }

    private val logger = logger(this)
    private var mapper = DeserializerFactory.getObjectMapper()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestEnvironmentConfig {
        mapper = p.codec as ObjectMapper
        val node = p.readValueAsTree<ObjectNode>()
        val fields = node.fields().asSequence().map { it.key to it.value }.toMap()
        val dockerCompose = fields["docker-compose"]!!.asText()
        val dockerConfig = ctxt.findInjectableValue("dockerConfig", null, null) as DockerConfig
        return TestEnvironmentConfig(
            dockerCompose = dockerCompose,
            containers = deserializeContainers(dockerConfig, dockerCompose),
            mockServers = fields["mock-servers"]?.let { deserializeMockServers(it) } ?: mapOf(),
        )
    }

    /**
     * Deserializes Mock Server instances to Map of `(mockServerName, mockServer)`.
     * Sets `name` field to `mockServerName` for each Mock Server.
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
    private fun deserializeContainers(dockerConfig: DockerConfig, dockerComposeFile: String): Map<String, Container> {
        val dockerComposeContent = FileUtils.readFileContentsFromResources(dockerComposeFile)
        val dockerCompose = DeserializerFactory.getYamlMapper().readTree(dockerComposeContent)
        val serviceNode = dockerCompose.get("services")
        require(serviceNode != null) { "Docker-Compose needs to have services defined." }
        val services = serviceNode.fields()
        return deserializeContainers(dockerConfig, services)
    }

    private fun deserializeContainers(
        dockerConfig: DockerConfig,
        services: Iterator<MutableMap.MutableEntry<String, JsonNode>>
    ): Map<String, Container> {
        val result = mutableMapOf<String, Container>()
        for (entry in services) {
            val node = entry.value as ObjectNode
            convertListToMap(node, "labels")
            convertListToMap(node, "environment")
            val labels = getImageLabels(node.get("labels"))
            node.put("name", entry.key)
            node.put("type", labels[CONTAINER_TYPE_KEY] ?: "MISC")
            node.put("predefinedUrl", labels[URL_KEY])
            node.put("pullPolicy", labels[PULL_POLICY_KEY] ?: dockerConfig.pullPolicy.name)
            node.put("hasHealthcheck", node.has("healthcheck"))
            setDatabaseProperties(node, labels, node.get("environment"), getDatabaseInitializationScripts(node.get("labels")))
            result[entry.key] = mapper.treeToValue(node, Container::class.java)
        }
        return result
    }

    private fun setDatabaseProperties(
        node: ObjectNode,
        labels: Map<String, String?>,
        environmentNode: JsonNode?,
        databaseInitializationScripts: Map<String, String>
    ) {
        if (node.get("type")?.asText() != "DATABASE") {
            return
        }

        val environment = environmentNode?.fields()?.asSequence()?.associate { (key, node) -> key to node.asText() } ?: mapOf()
        val system = labels[DATABASE_SYSTEM_KEY]?.let { DatabaseManagementSystem.valueOf(it) } ?: DatabaseManagementSystem.OTHER
        val schema = labels[DATABASE_SCHEMA_KEY]
        var database = labels[DATABASE_NAME_KEY]
        var username = labels[DATABASE_USERNAME_KEY]
        var password = labels[DATABASE_PASSWORD_KEY]

        if (database == null && system.environmentKeys.databaseName != null) {
            database = environment[system.environmentKeys.databaseName]
        }
        if (username == null && system.environmentKeys.username != null) {
            username = environment[system.environmentKeys.username]
        }
        if (password == null && system.environmentKeys.password != null) {
            password = environment[system.environmentKeys.password]
        }

        node.put("system", system.name)
        node.put("schema", schema)
        node.put("database", database)
        node.put("username", username)
        node.put("password", password)
        node.set<ObjectNode>("initializationScripts", databaseInitializationScripts.toJsonNode())
    }

    private fun getDatabaseInitializationScripts(labelsNode: JsonNode?): Map<String, String> {
        val fields = labelsNode?.fields() ?: return mapOf()
        val scripts = fields.asSequence().mapNotNull { (key, node) ->
            when (val match = DATABASE_INIT_SCRIPTS_KEY_REGEX.find(key)) {
                null -> null
                else -> match to node
            }
        }

        return scripts.associate { (match, node) -> match.groupValues[1] to node.asText() }
    }

    private fun getImageLabels(labelsNode: JsonNode?): Map<String, String?> {
        return mapOf(
            CONTAINER_TYPE_KEY to labelsNode?.get(CONTAINER_TYPE_KEY)?.asText(),
            URL_KEY to labelsNode?.get(URL_KEY)?.asText(),
            DATABASE_SYSTEM_KEY to labelsNode?.get(DATABASE_SYSTEM_KEY)?.asText(),
            DATABASE_NAME_KEY to labelsNode?.get(DATABASE_NAME_KEY)?.asText(),
            DATABASE_SCHEMA_KEY to labelsNode?.get(DATABASE_SCHEMA_KEY)?.asText(),
            DATABASE_USERNAME_KEY to labelsNode?.get(DATABASE_USERNAME_KEY)?.asText(),
            DATABASE_PASSWORD_KEY to labelsNode?.get(DATABASE_PASSWORD_KEY)?.asText(),
            PULL_POLICY_KEY to labelsNode?.get(PULL_POLICY_KEY)?.asText(),
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
     * @see <a href="https://docs.docker.com/compose/compose-file/compose-file-v3/#environment">Docker Documentation</a>
     */
    private fun convertListToMap(serviceNode: ObjectNode, key: String) {
        if (serviceNode[key] == null || !serviceNode[key].isArray) {
            return
        }

        val result = mutableMapOf<String, String>()
        for (entry in serviceNode[key].elements()) {
            val keyValuePair = entry.asText().split("=")
            if (keyValuePair.size != 2) {
                logger.warn("Ignoring entry ${entry.asText()} in $key since the entry is not in the required format.")
                continue
            }
            result[keyValuePair[0]] = keyValuePair[1]
        }
        serviceNode.replace(key, mapper.valueToTree(result))
    }

    private fun Map<String, String>.toJsonNode(): JsonNode {
        val node = JsonNodeFactory.instance.objectNode()
        for ((key, value) in this.entries) {
            node.put(key, value)
        }
        return node
    }
}
