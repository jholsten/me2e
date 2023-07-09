package org.jholsten.me2e.container

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType


/**
 * Model representing one Docker container.
 * This may be a microservice, a database or any other kind of supporting service.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.PROPERTY,
    defaultImpl = Container::class,
    visible = true,
)
@JsonSubTypes(value = [
    JsonSubTypes.Type(value = MicroserviceContainer::class, name = "MICROSERVICE")
])
open class Container(
    /**
     * Unique name of this container.
     */
    val name: String,
    
    /**
     * Type of this container.
     */
    val type: ContainerType,
    
    /**
     * Image to start the container from.
     */
    val image: String,

    /**
     * Environment variables for this container.
     */
    val environment: Map<String, String>? = null,
    
    // TODO: Add additional fields from docker-compose
) {
    /**
     * Starts this container and assigns the specified ports.
     * If a healthcheck is defined, it waits until the container is "healthy".
     */
    fun start() {
        // TODO
    }
    
    /**
     * Executes the given command inside the container.
     */
    fun execute() {
        // TODO
    }
}
