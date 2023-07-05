package org.jholsten.me2e.container.model

/**
 * Enum specifying the type of a Docker container.
 *
 * TODO: What about services like Kafka?
 * TODO: Maybe also mocked container for 3rd party services?
 */
enum class ContainerType {
    
    /**
     * Microservice offering an HTTP API.
     */
    MICROSERVICE,
    
    /**
     * Container containing resources used in the other services.
     * E.g. a database.
     */
    RESOURCE,
}
