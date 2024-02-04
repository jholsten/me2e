package org.jholsten.me2e.container.database

import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.connection.DatabaseConnection
import org.jholsten.me2e.container.database.connection.MongoDBConnection
import org.jholsten.me2e.container.database.connection.SQLDatabaseConnection
import org.jholsten.me2e.container.database.exception.DatabaseException
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.ContainerState
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant
import com.github.dockerjava.api.model.Container as DockerContainer

/**
 * Representation of a Docker container which contains a database.
 * Offers commands for interacting with the database, if the [system] is supported, i.e. if it is set to any value
 * other than [DatabaseManagementSystem.OTHER].
 */
class DatabaseContainer(
    /**
     * Unique name of this container.
     */
    name: String,

    /**
     * Image to start the container from.
     */
    image: String,

    /**
     * Environment variables for this container.
     */
    environment: Map<String, String>? = null,

    /**
     * Ports that should be exposed to localhost.
     */
    ports: ContainerPortList = ContainerPortList(),

    /**
     * Whether there is a healthcheck defined for this container in the Docker-Compose file.
     */
    hasHealthcheck: Boolean = false,

    /**
     * Pull policy for this Docker container.
     */
    pullPolicy: DockerConfig.PullPolicy = DockerConfig.PullPolicy.MISSING,

    /**
     * Database management system of this database container.
     */
    val system: DatabaseManagementSystem = DatabaseManagementSystem.OTHER,

    /**
     * Name of the database of this database container.
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val database: String? = null,

    /**
     * Name of the schema to which this database belongs.
     * Only applicable to SQL databases and only required for interacting with the database via the [DatabaseConnection].
     */
    val schema: String? = null,

    /**
     * Username to use for logging in.
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val username: String? = null,

    /**
     * Password to use for logging in.
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val password: String? = null,

    /**
     * Database initialization scripts to run when the container is started as map of `(name, path)`.
     * Only applicable if connection to the database via the [DatabaseConnection] could be established.
     */
    val initializationScripts: Map<String, String> = mapOf(),
) : Container(
    name = name,
    image = image,
    type = ContainerType.DATABASE,
    environment = environment,
    ports = ports,
    hasHealthcheck = hasHealthcheck,
    pullPolicy = pullPolicy,
) {
    private val logger = logger(this)

    /**
     * Connection to the database management system.
     * Only set for containers with systems other than [DatabaseManagementSystem.OTHER].
     */
    var connection: DatabaseConnection? = null

    /**
     * Executes all database initialization scripts given in [initializationScripts].
     * @throws IllegalStateException if connection to database is not established.
     */
    fun executeInitializationScripts() {
        if (initializationScripts.isEmpty()) {
            return
        }
        assertThatDatabaseConnectionIsEstablished()

        for ((name, script) in initializationScripts) {
            try {
                connection!!.executeScript(name, script)
                logger.info("Successfully executed database initialization script $name (located at $script).")
            } catch (e: FileNotFoundException) {
                logger.warn("Unable to find database initialization script $name (located at $script). Make sure it is on the classpath.")
            } catch (e: Exception) {
                logger.warn("Exception occurred while executing script $name (located at $script): ${e.message}")
            }
        }
    }

    /**
     * Returns list of all tables of the database. Excludes system tables such as `pg_class` in PostgreSQL.
     * In case of a No-SQL database, all collections are returned.
     * @throws IllegalStateException if connection to database is not established.
     */
    val tables: List<String>
        get() {
            assertThatDatabaseConnectionIsEstablished()
            return connection!!.tables
        }

    /**
     * Returns all entries which are currently stored in the table with the given name.
     * @param tableName Name of the table for which entries are to be retrieved.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun getAllFromTable(tableName: String): QueryResult {
        assertThatDatabaseConnectionIsEstablished()
        return connection!!.getAllFromTable(tableName)
    }

    /**
     * Executes the given script.
     * @param file File which contains the script.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun executeScript(file: File) {
        assertThatDatabaseConnectionIsEstablished()
        connection!!.executeScript(file)
    }

    /**
     * Executes the script on the given path. Path needs to be located in `resources` folder.
     * @param path Path to the file which contains the script. Needs to be located in `resources` folder.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun executeScript(path: String) {
        assertThatDatabaseConnectionIsEstablished()
        connection!!.executeScript(path)
    }

    /**
     * Truncates the tables with the given names.
     * @param tablesToClear Table names which should be cleared.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun clear(tablesToClear: List<String>) {
        assertThatDatabaseConnectionIsEstablished()
        connection!!.clear(tablesToClear)
    }

    /**
     * Truncates all tables of the connected database.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun clearAll() {
        assertThatDatabaseConnectionIsEstablished()
        connection!!.clearAll()
    }

    /**
     * Truncates all tables except the tables with the given names.
     * @param tablesToSkip Table names which should not be cleared.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun clearAllExcept(tablesToSkip: List<String>) {
        assertThatDatabaseConnectionIsEstablished()
        connection!!.clearAllExcept(tablesToSkip)
    }

    /**
     * Resets the whole database by deleting all tables, entries and sequences.
     * @throws IllegalStateException if connection to database is not established.
     */
    fun reset() {
        assertThatDatabaseConnectionIsEstablished()
        connection!!.reset()
    }

    @JvmSynthetic
    override fun initialize(dockerContainer: DockerContainer, state: ContainerState, environment: DockerCompose) {
        super.initialize(dockerContainer, state, environment)
        val exposedPort = ports.findFirstExposed()
        if (exposedPort?.external == null) {
            logger.warn("Could not find any exposed ports for database container '$name'.")
        } else if (database == null) {
            logger.warn("Could not detect the name of the database for container '$name'.")
        } else if (system != DatabaseManagementSystem.OTHER) {
            initializeConnection(state, exposedPort)
            executeInitializationScripts()
        }
    }

    @JvmSynthetic
    override fun onRestart(timestamp: Instant) {
        super.onRestart(timestamp)
        val exposedPort = ports.findFirstExposed()
        if (system != DatabaseManagementSystem.OTHER && exposedPort?.external != null && database != null) {
            initializeConnection(dockerContainer!!.state, exposedPort)
        }
    }


    /**
     * Initializes the connection to the database of this container.
     * @param state Reference to the corresponding Docker container state.
     * @param exposedPort Port on which the database is accessible.
     */
    private fun initializeConnection(state: ContainerState, exposedPort: ContainerPort) {
        if (system == DatabaseManagementSystem.MONGO_DB) {
            connection = MongoDBConnection.Builder()
                .withContainer(state)
                .withHost(state.host)
                .withPort(exposedPort.external!!)
                .withDatabase(database!!)
                .withUsername(username)
                .withPassword(password)
                .build()
        } else if (system.isSQL) {
            connection = SQLDatabaseConnection.Builder()
                .withHost(state.host)
                .withPort(exposedPort.external!!)
                .withSystem(system)
                .withDatabase(database!!)
                .withSchema(schema)
                .withUsername(username)
                .withPassword(password)
                .build()
        }

        logger.info(
            """
            Initialized connection to database container '$name': {
               system: $system, database: $database, schema: $schema, username: $username, password: $password
            }
            """.trimIndent()
        )
    }

    private fun assertThatDatabaseConnectionIsEstablished() {
        if (connection == null) {
            val message = when (system) {
                DatabaseManagementSystem.OTHER -> "Interaction with the database via the `connection` instance is only supported for " +
                    "a certain selection of database management systems, of which this system is not a part. " +
                    "To be able to use these methods, you can implement the `org.jholsten.me2e.container.database.DatabaseConnection` " +
                    "class yourself and set the connection attribute to an instance of this class."

                else -> "Unable to interact with the database, since a connection could not be established. Make sure that you have " +
                    "defined at least one port binding for the container and specified the name of the database in the " +
                    "`org.jholsten.me2e.database.name` label."
            }
            throw IllegalStateException(message)
        }
    }
}
