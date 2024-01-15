package org.jholsten.me2e.container.database

/**
 * Supported database management systems of a database container.
 * For all systems other than [OTHER], there are predefined commands for resetting the database state and inserting values.
 */
enum class DatabaseManagementSystem {
    MY_SQL,
    MS_SQL,
    POSTGRESQL,
    MARIA_DB,
    MONGO_DB,
    OTHER,
}
