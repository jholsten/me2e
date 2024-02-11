package org.jholsten.me2e.container.database.connection

import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.exception.DatabaseException
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.parsing.utils.FileUtils
import java.io.File

/**
 * Representation of the connection to an SQL or No-SQL database.
 * Allows to initialize, query and reset the state of a database instance.
 * @see SQLDatabaseConnection
 * @see MongoDBConnection
 */
abstract class DatabaseConnection protected constructor(
    /**
     * Hostname on which the database container is running.
     */
    val host: String,

    /**
     * Port on which the database container is accessible from [host].
     */
    val port: Int,

    /**
     * Name of the database to which the connection should be established.
     */
    val database: String,

    /**
     * Username to use for logging in.
     */
    val username: String?,

    /**
     * Password to use for logging in.
     */
    val password: String?,

    /**
     * Database management system which contains the database.
     */
    val system: DatabaseManagementSystem,
) {
    /**
     * Returns list of all tables of the database. Excludes system tables such as `pg_class` in PostgreSQL.
     * In case of a No-SQL database, all collections are returned.
     */
    abstract val tables: List<String>

    /**
     * Returns all entries which are currently stored in the table with the given name.
     * @param tableName Name of the table for which entries are to be retrieved.
     */
    abstract fun getAllFromTable(tableName: String): QueryResult

    /**
     * Executes the given script.
     * @param name Name of the script (for logging purposes).
     * @param file File which contains the script.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    abstract fun executeScript(name: String?, file: File)

    /**
     * Executes the given script.
     * @param file File which contains the script.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    fun executeScript(file: File) {
        executeScript(null, file)
    }

    /**
     * Executes the script located on the given path. Path needs to be located in `resources` folder.
     * @param name Name of the script (for logging purposes).
     * @param path Path to the file which contains the script. Needs to be located in `resources` folder.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    fun executeScript(name: String?, path: String) {
        executeScript(name, FileUtils.getResourceAsFile(path))
    }

    /**
     * Executes the script located on the given path. Path needs to be located in `resources` folder.
     * @param path Path to the file which contains the script. Needs to be located in `resources` folder.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    fun executeScript(path: String) {
        executeScript(null, path)
    }

    /**
     * Truncates the tables with the given names.
     * @param tablesToClear Table names which should be cleared.
     */
    abstract fun clear(tablesToClear: List<String>)

    /**
     * Truncates all tables of the connected database.
     */
    fun clearAll() {
        clear(tables)
    }

    /**
     * Truncates all tables except the tables with the given names.
     * @param tablesToSkip Table names which should not be cleared.
     */
    fun clearAllExcept(tablesToSkip: List<String>) {
        val tablesToClear = tables.filter { table -> tablesToSkip.none { table.lowercase() == it.lowercase() } }
        clear(tablesToClear)
    }

    /**
     * Resets the whole database by deleting all tables, entries and sequences.
     */
    abstract fun reset()

    /**
     * Closes the connection to the database.
     */
    abstract fun close()

    /**
     * Base class for builders for instantiating instances of [DatabaseConnection].
     */
    abstract class Builder<SELF : Builder<SELF>> {
        /**
         * Hostname on which the database container is running.
         * @see DatabaseConnection.host
         */
        protected var host: String? = null

        /**
         * Port on which the database container is accessible.
         * @see DatabaseConnection.port
         */
        protected var port: Int? = null

        /**
         * Name of the database to which the connection should be established.
         * @see DatabaseConnection.database
         */
        protected var database: String? = null

        /**
         * Username to use for logging in.
         * @see DatabaseConnection.username
         */
        protected var username: String? = null

        /**
         * Password to use for logging in.
         * @see DatabaseConnection.password
         */
        protected var password: String? = null

        /**
         * Sets the hostname on which the database container is running.
         * As this is a Docker container, the name of the host on which
         * the Docker engine is running should be entered here.
         * @param host Name of the host on which the container is running.
         * @return This builder instance, to use for chaining.
         */
        fun withHost(host: String): SELF {
            this.host = host
            return self()
        }

        /**
         * Sets the port on which the database container is accessible.
         * @param port Port number on which the container is accessible.
         * @return This builder instance, to use for chaining.
         */
        fun withPort(port: Int): SELF {
            this.port = port
            return self()
        }

        /**
         * Sets the name of the database to which the connection should be established.
         * @param database Name of the database.
         * @return This builder instance, to use for chaining.
         */
        fun withDatabase(database: String): SELF {
            this.database = database
            return self()
        }

        /**
         * Sets the username to use for logging in.
         * @param username Username to use for logging in to the database.
         * @see withPassword
         * @return This builder instance, to use for chaining.
         */
        fun withUsername(username: String?): SELF {
            this.username = username
            return self()
        }

        /**
         * Sets the password to use for logging in.
         * @param password Password to use for logging in to the database.
         * @see withUsername
         * @return This builder instance, to use for chaining.
         */
        fun withPassword(password: String?): SELF {
            this.password = password
            return self()
        }

        /**
         * Builds an instance of the [DatabaseConnection] using the properties set in this builder.
         */
        abstract fun build(): DatabaseConnection

        /**
         * Returns the instantiated Builder instance.
         * For subtypes of this class, this method should use the type of the subclass as the return type.
         * Only then will it be possible to chain method invocations of this class and the subclass.
         */
        private fun self(): SELF {
            @Suppress("UNCHECKED_CAST")
            return this as SELF
        }
    }
}
