package org.jholsten.me2e.container.model

/**
 * Enum specifying the type of a Docker container.
 */
enum class ContainerType {
    
    /**
     * Microservice offering an HTTP API.
     */
    MICROSERVICE,
    
    /**
     * Database container.
     */
    DATABASE,
    
    /**
     * Any other type of container that does not offer an API and is not a database.
     */
    MISC,
}
