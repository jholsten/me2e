package org.jholsten.me2e.container.database.connection

import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.container.database.model.SQLTableSpecification
import org.jholsten.me2e.container.database.model.TableSpecification

/**
 * Representation of the connection to an SQL or No-SQL database.
 */
abstract class DatabaseConnection(
    val username: String,
    val password: String,
) {
    /**
     * Returns list of all tables of the database.
     * In case of a No-SQL database, the collections are returned.
     */
    abstract val tables: List<TableSpecification>

    fun getAllFromTable(tableName: String): QueryResult {
        return getAllFromTable(tableName.toTableSpecification())
    }

    abstract fun getAllFromTable(table: TableSpecification): QueryResult

    /**
     * Truncates the given tables.
     */
    abstract fun clear(tablesToClear: List<TableSpecification>)

    @JvmName("clearTables")
    fun clear(tablesToClear: List<String>) {
        clear(tablesToClear.map { it.toTableSpecification() })
    }

    /**
     * Truncates all tables.
     */
    fun clearAll() {
        clear(tables)
    }

    /**
     * Truncates all tables except the given [tablesToSkip].
     */
    fun clearAllExcept(tablesToSkip: List<TableSpecification>) {
        val tablesToClear = tables.filter { !tablesToSkip.contains(it) }
        clear(tablesToClear)
    }

    private fun String.toTableSpecification(): TableSpecification {
        return when {
            this.contains(".") -> SQLTableSpecification(
                name = this.substringBefore("."),
                schema = this.substringAfter("."),
            )

            else -> TableSpecification(name = this)
        }
    }

}

// For NoSQL: https://hibernate.org/ogm/
// MongoDB: https://stackoverflow.com/questions/29370147/truncate-a-mongo-db-collection-from-java
// Get all collections: https://stackoverflow.com/questions/4971329/list-of-all-collections-in-mongo-database-in-java
