package org.jholsten.me2e.container.database

/**
 * Supported database management systems of a database container.
 * For all systems other than [OTHER], there are predefined commands for resetting the database state and inserting values.
 */
enum class DatabaseManagementSystem(
    /**
     * Names of the environment variables used to define properties of the database.
     */
    val environmentKeys: EnvironmentKeys,

    /**
     * Whether this database management system is an SQL system.
     */
    val isSQL: Boolean,
) {
    MY_SQL(
        EnvironmentKeys(
            databaseName = "MYSQL_DATABASE",
            username = "MYSQL_USER",
            password = "MYSQL_PASSWORD",
        ),
        isSQL = true,
    ),
    POSTGRESQL(
        EnvironmentKeys(
            databaseName = "POSTGRES_DB",
            username = "POSTGRES_USER",
            password = "POSTGRES_PASSWORD",
        ),
        isSQL = true,
    ),
    MARIA_DB(
        EnvironmentKeys(
            databaseName = "MYSQL_DATABASE",
            username = "MYSQL_USER",
            password = "MYSQL_PASSWORD",
        ),
        isSQL = true,
    ),
    MONGO_DB(
        EnvironmentKeys(
            databaseName = "MONGO_INITDB_DATABASE",
            username = "MONGO_INITDB_ROOT_USERNAME",
            password = "MONGO_INITDB_ROOT_PASSWORD",
        ),
        isSQL = false,
    ),
    OTHER(EnvironmentKeys(), isSQL = false);

    data class EnvironmentKeys(
        val databaseName: String? = null,
        val username: String? = null,
        val password: String? = null,
    )
}
