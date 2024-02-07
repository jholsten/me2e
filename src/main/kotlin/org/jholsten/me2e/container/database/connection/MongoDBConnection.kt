package org.jholsten.me2e.container.database.connection

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.exception.DatabaseException
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.utils.logger
import java.io.File
import java.io.FileNotFoundException

/**
 * Representation of the connection to a MongoDB database.
 * Allows to initialize, query and reset the state of a MongoDB database instance.
 */
class MongoDBConnection private constructor(
    /**
     * Hostname on which the database container is running.
     */
    host: String,

    /**
     * Port on which the database container is accessible from [host].
     */
    port: Int,

    /**
     * Name of the database to which the connection should be established.
     */
    database: String,

    /**
     * Username to use for logging in.
     */
    username: String? = null,

    /**
     * Password to use for logging in.
     */
    password: String? = null,

    /**
     * Settings to use for the connection to MongoDB.
     * The connection string is set to [url] upon initialization.
     */
    settings: MongoClientSettings?,

    /**
     * Reference to the Docker container which serves this database.
     * Is required to run scripts.
     */
    val container: Container?,
) : DatabaseConnection(host, port, database, username, password, DatabaseManagementSystem.MONGO_DB) {
    private val logger = logger(this)

    /**
     * MongoDB URL to use for connecting to the database.
     * Is composed of the address on which the database container is accessible and - if provided - the
     * credentials to use for logging in.
     */
    val url: String = when {
        username != null && password != null -> "mongodb://$username:$password@$host:$port"
        else -> "mongodb://$host:$port"
    }

    /**
     * Settings to use for the connection to MongoDB.
     * If specified, the settings provided in the constructor are used, otherwise the default [MongoClientSettings] are used.
     * The connection string is set to the value of the [url].
     */
    val settings: MongoClientSettings by lazy {
        val builder = when {
            settings != null -> MongoClientSettings.builder(settings)
            else -> MongoClientSettings.builder()
        }
        builder.applyConnectionString(ConnectionString(url)).build()
    }

    /**
     * Mongo client which is connecting to the database.
     */
    val client: MongoClient by lazy {
        MongoClients.create(this.settings)
    }

    /**
     * Connection to the database instance.
     */
    val connection: MongoDatabase by lazy {
        client.getDatabase(database)
    }

    /**
     * Command for executing shell commands inside the MongoDB container.
     * For versions < 5.0, the command is `mongo`, whereas for newer versions, the command is `mongosh`.
     * @see <a href="https://www.mongodb.com/docs/v4.4/mongo/#std-label-compare-mongosh-mongo">MongoDB Documentation</a>
     */
    val mongoShellCommand: String? by lazy {
        if (container == null) {
            null
        } else {
            getMongoCommand(container)
        }
    }

    override val tables: List<String>
        get() = connection.listCollectionNames().toList()

    override fun getAllFromTable(tableName: String): QueryResult {
        val result = connection.getCollection(tableName).find()
        val rows = mutableListOf<Map<String, Any?>>()
        for (entry in result) {
            rows.add(entry.toMutableMap())
        }
        return QueryResult(rows)
    }

    /**
     * Executes the given JavaScript script in the MongoDB Docker container.
     * As the Java-[MongoClient] does not offer the possibility to execute scripts, the script is executed within the
     * Docker container using the Mongo shell. To make this script available within the container, the file is first
     * copied into the container.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    override fun executeScript(name: String?, file: File) {
        checkNotNull(container) { "As scripts cannot be executed using the Java-MongoClient, the reference to the corresponding Docker container needs to be set." }
        checkNotNull(mongoShellCommand) { "Could not find Mongo on the PATH. Is it installed?" }
        val scriptName = name?.let { "$name (located at ${file.path})" } ?: file.path
        if (!file.exists()) {
            throw FileNotFoundException("File $scriptName does not exist.")
        }
        logger.info("Copying script $scriptName to container...")
        container.copyFileToContainer(file.path, file.name)
        logger.info("Executing script $scriptName...")
        val command = arrayOf(mongoShellCommand!!, "--quiet", "<", file.name)
        val result = container.execute(*command)
        if (result.exitCode != 0) {
            throw DatabaseException("Unable to execute script $scriptName (executed command: ${command.joinToString(" ")}): ${result.stdout}")
        }
    }

    override fun clear(tablesToClear: List<String>) {
        if (tablesToClear.isEmpty()) {
            return
        }

        logger.info("Clearing ${tablesToClear.size} collections...")
        for (table in tablesToClear) {
            val collection = connection.getCollection(table)
            collection.deleteMany(Document())
        }
    }

    override fun reset() {
        connection.drop()
    }

    override fun close() {
        client.close()
    }

    class Builder : DatabaseConnection.Builder<Builder>() {
        private var container: Container? = null
        private var settings: MongoClientSettings? = null

        /**
         * Sets reference to the Docker container which serves this database.
         * Is required to run scripts.
         * @param container Reference to the Docker container which serves this database.
         * @return Builder instance, to use for chaining.
         */
        fun withContainer(container: Container?) = apply {
            this.container = container
        }

        /**
         * Sets settings to use for the connection to MongoDB.
         * The connection string is set to [url] upon initialization.
         * @param settings Settings to use for the connection to MongoDB.
         * @return Builder instance, to use for chaining.
         */
        fun withSettings(settings: MongoClientSettings) = apply {
            this.settings = settings
        }

        override fun self(): Builder = this

        override fun build(): MongoDBConnection {
            return MongoDBConnection(
                host = requireNotNull(host) { "Host cannot be null" },
                port = requireNotNull(port) { "Port cannot be null" },
                database = requireNotNull(database) { "Database cannot be null" },
                username = username,
                password = password,
                settings = settings,
                container = container,
            )
        }
    }

    /**
     * Tries to find the command to use to interact with the Mongo shell.
     * For MongoDB versions < 5.0 the command is `mongo`, while for newer versions it is `mongosh`.
     * To find the correct command, both are tried out one after the other. If none of the commands
     * is set on the PATH, `null` is returned.
     */
    private fun getMongoCommand(container: Container): String? {
        val mongoshResult = container.execute("mongosh", "--version")
        if (mongoshResult.exitCode == 0) {
            logger.info("Using command 'mongosh' to execute commands on the Mongo shell.")
            return "mongosh"
        }
        val mongoResult = container.execute("mongo", "--version")
        if (mongoResult.exitCode == 0) {
            logger.info("Using command 'mongo' to execute commands on the Mongo shell.")
            return "mongo"
        }
        logger.warn("Neither 'mongo' nor 'mongosh' seem to be available on the PATH. Cannot execute commands on the Mongo shell.")
        return null
    }
}
