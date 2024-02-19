package org.jholsten.me2e.container.database.model

/**
 * Representation of the result of an executed query.
 * Contains the rows of the result as a map of column names and the corresponding value.
 */
class QueryResult internal constructor(rows: List<Map<String, Any?>>) : ArrayList<Map<String, Any?>>(rows) {
    /**
     * List of column names included in the result.
     * Note that the order of the columns is not necessarily the same as the order in the actual result.
     */
    val columns: List<String> = rows.flatMap { it.keys }.distinct()

    /**
     * Returns all entries of the result in column named [columnName].
     * @param columnName Name of the column for which entries should be retrieved.
     * @return Entries of the query result in column [columnName].
     */
    fun getEntriesInColumn(columnName: String): List<Any?> {
        return this.map { it[columnName] }
    }

    /**
     * Tries to find the row in this query result for which the entry in the column with the given
     * [columName] is equal to the given [value]. If multiple rows match this query, the first one
     * is returned. If no such row could be found, `null` is returned.
     * @param columName Name of the column for which matching row should be found.
     * @param value Value to find in the column with the given name.
     * @return Map of column name and value for the first row which contains the [value] in column [columName].
     */
    fun getByField(columName: String, value: Any): Map<String, Any?>? {
        return this.find { it[columName] == value }
    }
}
