package org.jholsten.me2e.config.parser.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.model.TestEnvironmentConfig
import org.jholsten.me2e.config.parser.DockerComposeValidator
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.mock.MockServer
import org.jholsten.me2e.parsing.utils.DeserializerFactory
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger

/**
 * Custom deserializer for parsing the test environment defined in the `environment` section of the config file
 * to an instance of [TestEnvironmentConfig]. Deserializes services in Docker-Compose file to [Container] instances
 * and sets the `name` of the Mock Servers to their corresponding key.
 *
 * Prior to executing this deserializer, the schema has already been validated using the
 * [org.jholsten.me2e.config.parser.ConfigSchemaValidator], so that it can be assumed that all required
 * fields are set for the environment definition.
 */
internal class TestEnvironmentConfigDeserializer : JsonDeserializer<TestEnvironmentConfig>() {
    companion object {
        /**
         * Label key for specifying the container type of a service.
         * @see Container.type
         */
        private const val CONTAINER_TYPE_KEY = "org.jholsten.me2e.container-type"

        /**
         * Label key for specifying the URL of a microservice.
         * @see org.jholsten.me2e.container.microservice.MicroserviceContainer.url
         */
        private const val URL_KEY = "org.jholsten.me2e.url"

        /**
         * Label key for specifying the database type (i.e. Database Management System) of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         * @see org.jholsten.me2e.container.database.DatabaseContainer.system
         */
        private const val DATABASE_SYSTEM_KEY = "org.jholsten.me2e.database.system"

        /**
         * Label key for specifying the name of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         * @see org.jholsten.me2e.container.database.DatabaseContainer.database
         */
        private const val DATABASE_NAME_KEY = "org.jholsten.me2e.database.name"

        /**
         * Label key for specifying the schema of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE] and SQL database management systems.
         * @see org.jholsten.me2e.container.database.DatabaseContainer.schema
         */
        private const val DATABASE_SCHEMA_KEY = "org.jholsten.me2e.database.schema"

        /**
         * Label key for specifying the username of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         * @see org.jholsten.me2e.container.database.DatabaseContainer.username
         */
        private const val DATABASE_USERNAME_KEY = "org.jholsten.me2e.database.username"

        /**
         * Label key for specifying the password of the database of a service.
         * Only applicable to services of type [ContainerType.DATABASE].
         * @see org.jholsten.me2e.container.database.DatabaseContainer.password
         */
        private const val DATABASE_PASSWORD_KEY = "org.jholsten.me2e.database.password"

        /**
         * Regex to match label keys for specifying initialization scripts for the database.
         * Only applicable to services of type [ContainerType.DATABASE].
         * @see org.jholsten.me2e.container.database.DatabaseContainer.initializationScripts
         */
        private val DATABASE_INIT_SCRIPTS_KEY_REGEX = Regex("org\\.jholsten\\.me2e\\.database\\.init-script\\.(.*)")

        /**
         * Label key for specifying which tables to skip when clearing the database.
         * Only applicable to services of type [ContainerType.DATABASE].
         * @see org.jholsten.me2e.container.database.DatabaseContainer.password
         */
        private const val DATABASE_RESET_SKIP_TABLES_KEY = "org.jholsten.me2e.database.reset.skip-tables"

        /**
         * Label key for specifying the pull policy for this container.
         * @see Container.pullPolicy
         */
        private const val PULL_POLICY_KEY = "org.jholsten.me2e.pull-policy"
    }

    private val logger = logger<TestEnvironmentConfigDeserializer>()

    /**
     * Object mapper to use for deserializing containers and Mock Servers.
     */
    private lateinit var mapper: ObjectMapper

    /**
     * Deserializes the JSON node containing the environment definition to an instance of [TestEnvironmentConfig].
     * Parses the services defined in the referenced Docker-Compose file to instances of [Container].
     * @return Deserialized test environment config.
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestEnvironmentConfig {
        mapper = p.codec as ObjectMapper
        val node = p.readValueAsTree<ObjectNode>()
        val fields = node.fields().asSequence().map { it.key to it.value }.toMap()
        val dockerCompose = fields["docker-compose"]!!.asText()
        val dockerConfig = ctxt.findInjectableValue(TestConfigDeserializer.INJECTABLE_DOCKER_CONFIG_FIELD_NAME, null, null) as DockerConfig
        return TestEnvironmentConfig(
            dockerCompose = dockerCompose,
            containers = deserializeContainers(dockerConfig, dockerCompose),
            mockServers = fields["mock-servers"]?.let { deserializeMockServers(it) } ?: mapOf(),
        )
    }

    /**
     * Deserializes Mock Server instances to Map of `(mockServerName, mockServer)`.
     * Sets `name` field to `mockServerName` for each Mock Server.
     * @return Deserialized map of `(mockServerName, mockServer)`.
     * @see MockServerDeserializer
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
     * Validates the contents of the given Docker-Compose file and then deserializes the services to
     * Map of `(containerName, container)`. Reads configuration properties from labels.
     * @return Deserialized map of `(containerName, container)`.
     */
    private fun deserializeContainers(dockerConfig: DockerConfig, dockerComposePath: String): Map<String, Container> {
        val dockerComposeFile = FileUtils.getResourceAsFile(dockerComposePath)
        DockerComposeValidator().validate(dockerComposeFile)
        val dockerCompose = DeserializerFactory.getYamlMapper().readTree(dockerComposeFile.readText())
        val serviceNode = dockerCompose.get("services")
        require(serviceNode != null) { "Docker-Compose needs to have services defined." }
        val services = serviceNode.fields()
        return deserializeContainers(dockerConfig, services)
    }

    /**
     * Deserializes the given map of services defined in the Docker-Compose file to a map of
     * `(containerName, container)`. Sets `name` field to `containerName` for each Container and reads
     * the configuration properties from the labels defined for each service.
     * @return Deserialized map of `(containerName, container)`.
     */
    private fun deserializeContainers(
        dockerConfig: DockerConfig,
        services: Iterator<MutableMap.MutableEntry<String, JsonNode>>
    ): Map<String, Container> {
        val result = mutableMapOf<String, Container>()
        for ((key, jsonNode) in services) {
            val node = jsonNode as ObjectNode
            convertListToMap(node, "labels")
            convertListToMap(node, "environment")
            val labels = getImageLabels(node.get("labels"))
            node.put("name", key)
            node.put("type", labels[CONTAINER_TYPE_KEY] ?: "MISC")
            node.put("predefinedUrl", labels[URL_KEY])
            node.put("pullPolicy", labels[PULL_POLICY_KEY] ?: dockerConfig.pullPolicy.name)
            setDatabaseProperties(node, labels, node.get("environment"), getDatabaseInitializationScripts(node.get("labels")))
            result[key] = mapper.treeToValue(node, Container::class.java)
        }
        return result
    }

    /**
     * Reads database configuration properties from the labels of the service represented by the given node.
     * Tries to read properties from the environment definition if labels are not defined.
     */
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
        val system = labels.getDatabaseManagementSystem()
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

        var tablesToSkipOnReset: List<String> = listOf()
        val tablesValue = labels[DATABASE_RESET_SKIP_TABLES_KEY]
        if (tablesValue != null) {
            tablesToSkipOnReset = tablesValue.split("[\\s,]+".toRegex())
        }

        node.put("system", system.name)
        node.put("schema", schema)
        node.put("database", database)
        node.put("username", username)
        node.put("password", password)
        node.set<ObjectNode>("initializationScripts", databaseInitializationScripts.toJsonNode())
        node.set<ObjectNode>("tablesToSkipOnReset", tablesToSkipOnReset.toArrayNode())
    }

    /**
     * Returns database initialization scripts defined in the given `labels` node.
     * Each initialization script is composed of a name and the path to the corresponding file.
     * @return Map of `name` and `path`
     */
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

    /**
     * Reads all configuration properties from the `labels` defined in the given node.
     * @return Map of `labelName` and `labelValue`.
     */
    private fun getImageLabels(labelsNode: JsonNode?): Map<String, String?> {
        return mapOf(
            CONTAINER_TYPE_KEY to labelsNode?.get(CONTAINER_TYPE_KEY)?.asText(),
            URL_KEY to labelsNode?.get(URL_KEY)?.asText(),
            DATABASE_SYSTEM_KEY to labelsNode?.get(DATABASE_SYSTEM_KEY)?.asText(),
            DATABASE_NAME_KEY to labelsNode?.get(DATABASE_NAME_KEY)?.asText(),
            DATABASE_SCHEMA_KEY to labelsNode?.get(DATABASE_SCHEMA_KEY)?.asText(),
            DATABASE_USERNAME_KEY to labelsNode?.get(DATABASE_USERNAME_KEY)?.asText(),
            DATABASE_PASSWORD_KEY to labelsNode?.get(DATABASE_PASSWORD_KEY)?.asText(),
            DATABASE_RESET_SKIP_TABLES_KEY to labelsNode?.get(DATABASE_RESET_SKIP_TABLES_KEY)?.asText(),
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

        val result = mutableMapOf<String, String?>()
        for (entry in serviceNode[key].elements()) {
            val keyValuePair = entry.asText().split("=", limit = 2)
            when (keyValuePair.size) {
                1 -> result[keyValuePair[0]] = null
                2 -> result[keyValuePair[0]] = keyValuePair[1]
                else -> logger.warn("Ignoring entry ${entry.asText()} in $key since the entry is not in the required format.")
            }
        }
        serviceNode.replace(key, mapper.valueToTree(result))
    }

    /**
     * Reads the database management system from the given map of labels.
     * If system is not specified or if it is not part of the [DatabaseManagementSystem] enum values, [DatabaseManagementSystem.OTHER]
     * is returned. Otherwise, the value is parsed to the corresponding [DatabaseManagementSystem].
     */
    private fun Map<String, String?>.getDatabaseManagementSystem(): DatabaseManagementSystem {
        val entry = this[DATABASE_SYSTEM_KEY]
        if (entry == null || entry !in DatabaseManagementSystem.values().map { it.name }) {
            return DatabaseManagementSystem.OTHER
        }
        return DatabaseManagementSystem.valueOf(entry)
    }

    /**
     * Transforms the given Map to a JSON node.
     */
    private fun Map<String, String>.toJsonNode(): JsonNode {
        val node = JsonNodeFactory.instance.objectNode()
        for ((key, value) in this.entries) {
            node.put(key, value)
        }
        return node
    }

    /**
     * Transforms the given list of Strings to an array node.
     */
    private fun List<String>.toArrayNode(): JsonNode {
        val node = JsonNodeFactory.instance.arrayNode()
        this.forEach { node.add(it) }
        return node
    }
}
