package org.jholsten.me2e.container.database.connection

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.jholsten.me2e.container.database.DatabaseManagementSystem
import org.jholsten.me2e.container.database.model.QueryResult
import org.jholsten.me2e.utils.logger

/**
 * Representation of the connection to a Mongo-DB database.
 * Allows to query and reset the state of a database instance.
 */
class MongoDBConnection(
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
    username: String? = null,

    /**
     * Password to use for logging in.
     */
    password: String? = null,
) : DatabaseConnection(host, port, database, username, password, DatabaseManagementSystem.MONGO_DB) {

    private val logger = logger(this)

    /**
     * URL to use for connecting to the database.
     */
    val url: String = when {
        username != null && password != null -> "mongodb://$username:$password@$host:$port"
        else -> "mongodb://$host:$port"
    }

    /**
     * Client which is connecting to the database.
     */
    val client: MongoClient by lazy {
        MongoClients.create(url)
    }

    /**
     * Connection to the database instance.
     */
    val connection: MongoDatabase by lazy {
        client.getDatabase(database)
    }

    override val tables: List<String>
        get() = connection.listCollectionNames().toList()

    override fun getAllFromTable(tableName: String): QueryResult {
        val result = connection.getCollection(tableName).find()
        val rows = mutableListOf<Map<String, Any?>>()
        for (entry in result) {
            rows.add(entry.toMutableMap())
        }
        return QueryResult(rows)
    }

    override fun clear(tablesToClear: List<String>) {
        if (tablesToClear.isEmpty()) {
            return
        }

        logger.info("Clearing ${tablesToClear.size} collections...")
        for (table in tablesToClear) {
            val collection = connection.getCollection(table)
            collection.deleteMany(Document())
        }
    }

    class Builder : DatabaseConnection.Builder<Builder>() {
        override fun self(): Builder = this

        override fun build(): MongoDBConnection {
            return MongoDBConnection(
                host = requireNotNull(host),
                port = requireNotNull(port),
                database = requireNotNull(database),
                username = username,
                password = password,
            )
        }
    }
}
