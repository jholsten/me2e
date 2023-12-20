package org.jholsten.me2e.container.database

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.container.model.ContainerType

/**
 * Model representing one database container.
 * Offers commands for resetting the state and inserting data.
 */
class DatabaseContainer(
    name: String,
    image: String,
    environment: Map<String, String>? = null,
    val system: DatabaseManagementSystem,
) : Container(
    name = name,
    image = image,
    type = ContainerType.DATABASE,
    environment = environment,
) {
    /**
     * TODO: Desired interface:
     * @Database("backendDB")
     * private val backendDB: Database
     *
     * backendDB.reset(exceptTables("A", "B"))
     * backendDB.runScript("abc.sql")
     */
}
