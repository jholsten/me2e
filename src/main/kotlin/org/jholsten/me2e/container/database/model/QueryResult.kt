package org.jholsten.me2e.container.database.model

/**
 * Representation of the result of an executed query.
 * Contains the rows of the result as a map of column names and the corresponding value.
 */
class QueryResult(rows: List<Map<String, Any?>>) : ArrayList<Map<String, Any?>>(rows)
