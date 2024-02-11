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

    /**
     * Keys that are used in the Docker-Compose to define certain properties of a database container as environment variables.
     * If the database name, username and/or password are not set in the corresponding labels, an attempt is made to read these
     * values from the corresponding environment variables.
     */
    data class EnvironmentKeys(
        /**
         * Name of the environment key which is used to specify the name of the database.
         */
        val databaseName: String? = null,

        /**
         * Name of the environment key which is used to specify the username to log into the database.
         */
        val username: String? = null,

        /**
         * Name of the environment key which is used to specify the password to log into the database.
         */
        val password: String? = null,
    )
}
