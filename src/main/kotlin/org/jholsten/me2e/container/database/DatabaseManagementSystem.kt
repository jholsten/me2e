package org.jholsten.me2e.container.database

/**
 * Supported database management systems of a database container.
 * For all systems other than [OTHER], there are predefined commands for resetting the database state and inserting values.
 */
enum class DatabaseManagementSystem {
    ORACLE,
    MY_SQL,
    MICROSOFT_SQL_SERVER,
    POSTGRESQL,
    MONGO_DB,
    MARIA_DB,
    OTHER,
}
