package org.jholsten.me2e.container

import org.jholsten.me2e.container.model.ContainerType

/**
 * Model representing one Docker container.
 * This may be a microservice, a database or any other kind of supporting service.
 */
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
    val environment: Map<String, String>?,
    
    // TODO: Add additional fields from docker-compose
) {
    /**
     * Starts this container and waits until it is healthy.
     */
    fun start() {
        // TODO
    }
}
