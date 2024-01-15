package org.jholsten.me2e.container.database.connection

import org.apache.ibatis.jdbc.RuntimeSqlException
import org.apache.ibatis.jdbc.ScriptRunner
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.database.exception.DatabaseException
import org.jholsten.me2e.container.database.model.SQLTableSpecification
import org.jholsten.me2e.container.database.model.TableSpecification
import org.jholsten.me2e.parsing.utils.FileUtils
import org.jholsten.me2e.utils.logger
import java.io.File
import java.io.FileReader
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class SQLDatabaseConnection(
    val jdbcUrl: String,
    username: String,
    password: String,
    val system: DatabaseManagementSystem,
) : DatabaseConnection(username, password) {

    private val logger = logger(this)

    /**
     * JDBC connection to the database.
     */
    val connection: Connection by lazy {
        connect()
    }

    override val tables: List<SQLTableSpecification>
        get() = fetchTables()

    override fun getAllFromTable(table: TableSpecification): QueryResult {
        connection.createStatement().runWithAutoRollback { statement ->
            statement.executeQuery("SELECT * FROM ${table.representation}").use { result ->
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
    fun executeScript(file: File) {
        logger.info("Executing SQL script ${file.path}...")
        try {
            val scriptRunner = ScriptRunner(connection)
            scriptRunner.setSendFullScript(false)
            scriptRunner.setStopOnError(true)
            scriptRunner.runScript(FileReader(file))
        } catch (e: RuntimeSqlException) {
            throw DatabaseException("Unable to execute script: ${e.message}")
        }
    }

    /**
     * Executes the SQL script on the given path.
     * Path needs to be located in `resources` folder.
     * @throws java.io.FileNotFoundException if file does not exist.
     * @throws DatabaseException if script could not be executed.
     */
    fun executeScript(path: String) {
        executeScript(FileUtils.getResourceAsFile(path))
    }

    override fun clear(tablesToClear: List<TableSpecification>) {
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

    /**
     * TODO: MySQL = db-Name, Postgres=public
     * TODO: MySQL case sensitive
     */
    fun clearAllFromSchema(schema: String) {
        clearAllFromSchemaExcept(schema, listOf())
    }

    fun clearAllFromSchemaExcept(schema: String, tablesToSkip: List<TableSpecification>) {
        val tablesToClear = fetchTables(schema).filter { !tablesToSkip.contains(it) }
        clear(tablesToClear)
    }

    private fun truncateTable(statement: Statement, table: TableSpecification) {
        var command = "TRUNCATE TABLE ${table.representation}"
        if (system == DatabaseManagementSystem.POSTGRESQL) {
            command += " CASCADE"
        }
        statement.executeUpdate(command)
    }

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
            DatabaseManagementSystem.MS_SQL -> "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            else -> throw DatabaseException("Unsupported system: $system")
        }
        if (DriverManager.drivers().noneMatch { it.javaClass.name == driver }) {
            Class.forName(driver)
            logger.info("Registered driver $driver for database system $system")
        }
    }

    private fun fetchTables(schema: String? = null): MutableList<SQLTableSpecification> {
        connection.createStatement().runWithAutoRollback { statement ->
            var query =
                "SELECT table_name, table_schema FROM INFORMATION_SCHEMA.TABLES WHERE (table_type='TABLE' OR table_type='BASE TABLE')"
            if (schema != null) {
                query += " AND table_schema='$schema'"
            }
            statement.executeQuery(query).use { result ->
                val tables = mutableListOf<SQLTableSpecification>()
                while (result.next()) {
                    tables.add(
                        SQLTableSpecification(
                            name = result.getString(1),
                            schema = result.getString(2),
                        )
                    )
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
        } else if (system == DatabaseManagementSystem.MS_SQL) {
            statement.execute("EXEC sp_MSForEachTable 'ALTER TABLE ? NOCHECK CONSTRAINT all';")
        }
    }

    private fun enableForeignKeyChecks(statement: Statement) {
        if (system == DatabaseManagementSystem.MY_SQL || system == DatabaseManagementSystem.MARIA_DB) {
            statement.execute("SET @@foreign_key_checks = 1;")
        } else if (system == DatabaseManagementSystem.MS_SQL) {
            statement.execute("EXEC sp_MSForEachTable 'ALTER TABLE ? CHECK CONSTRAINT ALL';")
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
            connection.rollback()
            throw e
        } finally {
            this.close()
        }
    }
}
