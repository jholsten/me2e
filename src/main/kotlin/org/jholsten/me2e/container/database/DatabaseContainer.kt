package org.jholsten.me2e.container.database

import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.connection.DatabaseConnection
import org.jholsten.me2e.container.database.connection.MongoDBConnection
import org.jholsten.me2e.container.database.connection.SQLDatabaseConnection
import org.jholsten.me2e.container.database.exception.DatabaseException
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.model.ContainerPort
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.ContainerState
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant

/**
 * Representation of a Docker container which contains a database.
 * Offers commands for interacting with the database, if the [system] is supported, i.e. if it is set to any value
 * other than [DatabaseManagementSystem.OTHER].
 */
class DatabaseContainer internal constructor(
    /**
     * Unique name of this container.
     */
    name: String,

    /**
     * Image to start the container from.
     * Corresponds to the value given for the `image` keyword in Docker-Compose.
     */
    image: String?,

    /**
     * Environment variables for this container.
     * Corresponds to the values given in the `environment` section of the Docker-Compose.
     */
    environment: Map<String, String>? = null,

    /**
     * Ports that should be exposed to localhost.
     * Corresponds to the `ports` section of the Docker-Compose.
     */
    ports: ContainerPortList = ContainerPortList(),

    /**
     * Pull policy for this Docker container.
     * If not overwritten in the label `org.jholsten.me2e.pull-policy` for this container, the global
     * pull policy [org.jholsten.me2e.config.model.DockerConfig.pullPolicy] is used.
     * @see org.jholsten.me2e.config.model.DockerConfig.pullPolicy
     */
    pullPolicy: DockerConfig.PullPolicy = DockerConfig.PullPolicy.MISSING,

    /**
     * Database management system of this database container.
     * Corresponds to the value of the label `org.jholsten.me2e.database.system` in the Docker-Compose.
     */
    val system: DatabaseManagementSystem = DatabaseManagementSystem.OTHER,

    /**
     * Name of the database of this database container.
     * Corresponds to the value of the label `org.jholsten.me2e.database.name` in the Docker-Compose or - if this value is not
     * set - the value from the corresponding environment variable (see [DatabaseManagementSystem.EnvironmentKeys.databaseName]).
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val database: String? = null,

    /**
     * Name of the schema to which this database belongs.
     * Corresponds to the value of the label `org.jholsten.me2e.database.schema` in the Docker-Compose.
     * Only applicable to SQL databases and only required for interacting with the database via the [DatabaseConnection].
     */
    val schema: String? = null,

    /**
     * Username to use for logging in.
     * Corresponds to the value of the label `org.jholsten.me2e.database.username` in the Docker-Compose or - if this value is not
     * set - the value from the corresponding environment variable (see [DatabaseManagementSystem.EnvironmentKeys.username]).
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val username: String? = null,

    /**
     * Password to use for logging in.
     * Corresponds to the value of the label `org.jholsten.me2e.database.password` in the Docker-Compose or - if this value is not
     * set - the value from the corresponding environment variable (see [DatabaseManagementSystem.EnvironmentKeys.password]).
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val password: String? = null,

    /**
     * Database initialization scripts to run when the container is started as map of `(name, path)`.
     * Corresponds to the values of the labels with pattern `org.jholsten.me2e.database.init-script.$name` in the Docker-Compose.
     * Only applicable if connection to the database via the [DatabaseConnection] could be established.
     * Note that these initialization scripts are executed after clearing all database tables in
     * [org.jholsten.me2e.Me2eExtension.clearDatabases] to restore the original state of the database.
     */
    val initializationScripts: Map<String, String> = mapOf(),

    /**
     * List of table names to skip when automatically clearing the database after each test (see [org.jholsten.me2e.Me2eExtension.clearDatabases]).
     * Corresponds to the list of comma separated values of the label `org.jholsten.me2e.database.reset.skip-tables` in the Docker-Compose.
     * Only required for interacting with the database via the [DatabaseConnection].
     */
    val tablesToSkipOnReset: List<String> = listOf(),
) : Container(
    name = name,
    image = image,
    type = ContainerType.DATABASE,
    environment = environment,
    ports = ports,
    pullPolicy = pullPolicy,
) {
    private val logger = logger<DatabaseContainer>()

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

    /**
     * Callback function to execute when all containers of the environment are healthy.
     * As a connection to the database can only be established after the database is up and running, the connection
     * attempt is only made as soon as all containers of the environment are healthy. Otherwise, the initialization
     * scripts cannot be executed.
     */
    @JvmSynthetic
    override fun initializeOnContainerHealthy() {
        super.initializeOnContainerHealthy()
        val exposedPort = ports.findFirstExposed()
        if (exposedPort?.external == null) {
            logger.warn("Could not find any exposed ports for database container '$name'.")
        } else if (database == null) {
            logger.warn("Could not detect the name of the database for container '$name'.")
        } else if (system != DatabaseManagementSystem.OTHER) {
            initializeConnection(dockerContainer!!.state, exposedPort)
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
                .withContainer(this)
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
