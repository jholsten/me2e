package org.jholsten.me2e.container.database.exception

/**
 * Exception that is thrown on failures when interacting with a database.
 */
class DatabaseException internal constructor(message: String) : RuntimeException(message)
