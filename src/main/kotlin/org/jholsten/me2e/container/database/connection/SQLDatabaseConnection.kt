package org.jholsten.me2e.container.database.connection

import org.apache.ibatis.jdbc.RuntimeSqlException
import org.apache.ibatis.jdbc.ScriptRunner
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.database.exception.DatabaseException
import org.jholsten.me2e.utils.logger
import java.io.File
import java.io.FileReader
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

/**
 * Representation of the connection to an SQL database.
 * Allows to query and reset the state of a database instance.
 */
class SQLDatabaseConnection private constructor(
    /**
     * Hostname on which the database container is running.
     */
    host: String,

    /**
     * Port on which the database container is running.
     */
    port: Int,

    /**
     * Name of the database to which the connection should be established.
     */
    database: String,

    /**
     * Username to use for logging in.
     */
    username: String?,

    /**
     * Password to use for logging in.
     */
    password: String?,

    /**
     * Database management system which contains the database.
     */
    system: DatabaseManagementSystem,

    /**
     * Name of the schema to which this database belongs.
     * In case of [DatabaseManagementSystem.MY_SQL] and [DatabaseManagementSystem.MARIA_DB], this is the name of the database.
     * For [DatabaseManagementSystem.POSTGRESQL], the schema is different to the database and is set to `public` by default.
     */
    val schema: String,
) : DatabaseConnection(host, port, database, username, password, system) {

    private val logger = logger(this)

    /**
     * JDBC URL to use for connecting to the database.
     */
    val jdbcUrl: String = "jdbc:${system.getJdbcDriverName()}://$host:$port/$database"

    /**
     * JDBC connection to the database.
     */
    val connection: Connection by lazy {
        connect()
    }

    override val tables: List<String>
        get() = fetchTables()

    override fun getAllFromTable(tableName: String): QueryResult {
        connection.createStatement().runWithAutoRollback { statement ->
            statement.executeQuery("SELECT * FROM ${getTableRepresentation(tableName)}").use { result ->
                val columnNames = (1 until result.metaData.columnCount + 1).associateWith { result.metaData.getColumnName(it) }
                val rows = mutableListOf<Map<String, Any?>>()
                while (result.next()) {
                    val row = mutableMapOf<String, Any?>()
                    for (i in 1 until result.metaData.columnCount + 1) {
                        row[columnNames[i]!!] = result.getObject(i)
                    }
                    rows.add(row)
                }
                return QueryResult(rows)
            }
        }
    }

    /**
     * Executes the given SQL script.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    override fun executeScript(name: String?, file: File) {
        val scriptName = name?.let { "$name (located at ${file.path})" } ?: file.path
        logger.info("Executing SQL script $scriptName...")
        try {
            val scriptRunner = ScriptRunner(connection)
            scriptRunner.setSendFullScript(false)
            scriptRunner.setStopOnError(true)
            scriptRunner.runScript(FileReader(file))
        } catch (e: RuntimeSqlException) {
            throw DatabaseException("Unable to execute script $scriptName: ${e.message}")
        }
    }

    override fun clear(tablesToClear: List<String>) {
        if (tablesToClear.isEmpty()) {
            return
        }

        logger.info("Clearing ${tablesToClear.size} tables...")
        connection.createStatement().runWithAutoRollback { statement ->
            disableForeignKeyChecks(statement)
            for (table in tablesToClear) {
                truncateTable(statement, table)
            }
            enableForeignKeyChecks(statement)
        }
    }

    override fun reset() {
        connection.createStatement().runWithAutoRollback { statement ->
            if (system == DatabaseManagementSystem.POSTGRESQL) {
                statement.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;")
            } else if (system == DatabaseManagementSystem.MY_SQL || system == DatabaseManagementSystem.MARIA_DB) {
                statement.execute("DROP DATABASE $database;")
                statement.execute("CREATE DATABASE $database;")
            } else {
                throw DatabaseException("Unsupported system: $system")
            }
            logger.info("Successfully reset database $database.")
        }
    }

    class Builder : DatabaseConnection.Builder<Builder>() {
        private val logger = logger(this)

        private var schema: String? = null
        private var system: DatabaseManagementSystem? = null

        /**
         * Sets the database management system which contains the database.
         */
        fun withSystem(system: DatabaseManagementSystem) = apply {
            this.system = system
        }

        /**
         * Sets the name of the schema to which this database belongs.
         * In case of [DatabaseManagementSystem.MY_SQL] and [DatabaseManagementSystem.MARIA_DB], this should be the name of the database.
         * For [DatabaseManagementSystem.POSTGRESQL], the schema is different to the database and is set to `public` by default.
         */
        fun withSchema(schema: String?) = apply {
            this.schema = schema
        }

        override fun self(): Builder = this

        override fun build(): SQLDatabaseConnection {
            if (this.system == DatabaseManagementSystem.POSTGRESQL) {
                this.schema = this.schema ?: "public"
            } else {
                if (schema != null) {
                    logger.warn(
                        """
                        Setting a custom database schema is only applicable for PostgreSQL databases. 
                        For other systems, the name of the database needs to be used as the schema. 
                        Will set this value to database name '$database'.
                        """.trimIndent()
                    )
                }
                this.schema = this.database
            }

            return SQLDatabaseConnection(
                host = requireNotNull(host),
                port = requireNotNull(port),
                database = requireNotNull(database),
                username = username,
                password = password,
                system = requireNotNull(system),
                schema = requireNotNull(schema),
            )
        }
    }

    /**
     * Truncates the table with the given name.
     */
    private fun truncateTable(statement: Statement, table: String) {
        var command = "TRUNCATE TABLE ${getTableRepresentation(table)}"
        if (system == DatabaseManagementSystem.POSTGRESQL) {
            command += " CASCADE"
        }
        statement.executeUpdate(command)
    }

    /**
     * Established JDBC connection to the database and registers the
     * corresponding driver for the [system].
     */
    private fun connect(): Connection {
        registerDriver()
        val connection = DriverManager.getConnection(jdbcUrl, username, password)
        logger.info("Established connection to database $jdbcUrl.")
        return connection
    }

    /**
     * Registers driver for the corresponding database management system by loading the class.
     */
    private fun registerDriver() {
        val driver = when (this.system) {
            DatabaseManagementSystem.MY_SQL -> "com.mysql.cj.jdbc.Driver"
            DatabaseManagementSystem.POSTGRESQL -> "org.postgresql.Driver"
            DatabaseManagementSystem.MARIA_DB -> "org.mariadb.jdbc.Driver"
            else -> throw DatabaseException("Unsupported system: $system")
        }
        Class.forName(driver)
        logger.info("Registered driver $driver for database system $system")
    }

    /**
     * Fetches the names of all tables belonging to the [database] and [schema].
     */
    private fun fetchTables(): MutableList<String> {
        connection.createStatement().runWithAutoRollback { statement ->
            statement.executeQuery(
                """
                SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE (table_type='TABLE' OR table_type='BASE TABLE') AND table_schema='$schema'
                """.trimIndent()
            ).use { result ->
                val tables = mutableListOf<String>()
                while (result.next()) {
                    tables.add(result.getString(1))
                }
                return tables
            }
        }
    }

    /**
     * When truncating a table with foreign keys, an error occurs if the referenced entries have not yet been deleted.
     * In PostgreSQL, this problem can be solved by adding `CASCADE` to the `TRUNCATE TABLE` command.
     * For other database management systems, however, the foreign key checks need to be disabled. After the tables
     * have been truncated, the foreign key checks are enabled again.
     */
    private fun disableForeignKeyChecks(statement: Statement) {
        if (system == DatabaseManagementSystem.MY_SQL || system == DatabaseManagementSystem.MARIA_DB) {
            statement.execute("SET @@foreign_key_checks = 0;")
        }
    }

    private fun enableForeignKeyChecks(statement: Statement) {
        if (system == DatabaseManagementSystem.MY_SQL || system == DatabaseManagementSystem.MARIA_DB) {
            statement.execute("SET @@foreign_key_checks = 1;")
        }
    }

    /**
     * Returns the representation of the given table to use for executing commands.
     * Concatenates [schema] and [tableName] to the representation required by the [system].
     */
    private fun getTableRepresentation(tableName: String): String {
        return when (this.system) {
            DatabaseManagementSystem.POSTGRESQL -> "$database.$schema.$tableName"
            else -> "$schema.$tableName"
        }
    }

    private fun DatabaseManagementSystem.getJdbcDriverName(): String {
        return when (this) {
            DatabaseManagementSystem.POSTGRESQL -> "postgresql"
            DatabaseManagementSystem.MY_SQL -> "mysql"
            DatabaseManagementSystem.MARIA_DB -> "mysql"
            else -> throw DatabaseException("Unsupported system: $system")
        }
    }

    /**
     * Executes [block] using this [Statement].
     * Rolls back the connection on exceptions and closes the statement.
     */
    private inline fun <T> Statement.runWithAutoRollback(block: (Statement) -> T): T {
        try {
            return block(this)
        } catch (e: Exception) {
            if (!connection.autoCommit) {
                connection.rollback()
            }
            throw e
        } finally {
            this.close()
        }
    }
}
