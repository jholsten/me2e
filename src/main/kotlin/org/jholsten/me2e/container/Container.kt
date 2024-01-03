package org.jholsten.me2e.container

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.utils.ContainerPortListDeserializer
import org.jholsten.me2e.container.database.DatabaseContainer
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
    JsonSubTypes.Type(value = MicroserviceContainer::class, name = "MICROSERVICE"),
    JsonSubTypes.Type(value = DatabaseContainer::class, name = "DATABASE"),
])
open class Container(
    /**
     * Unique name of this container.
     */
    val name: String,

    /**
     * Type of this container.
     */
    val type: ContainerType = ContainerType.MISC,

    /**
     * Image to start the container from.
     */
    val image: String,

    /**
     * Environment variables for this container.
     */
    val environment: Map<String, String>? = null,

    /**
     * Whether this container should be accessible from localhost.
     */
    val public: Boolean = false,

    /**
     * Ports that should be exposed to localhost.
     */
    val ports: ContainerPortList = ContainerPortList(),
) {
    @JsonDeserialize(using = ContainerPortListDeserializer::class)
    class ContainerPortList(ports: List<ContainerPort> = listOf()) : ArrayList<ContainerPort>(ports) {
    }

    /**
     * Representation of a port mapping from container-internal port
     * to port that is accessible from localhost.
     */
    class ContainerPort(
        /**
         * Container-internal port to be exposed.
         */
        val internal: Int,
        /**
         * Port from which container is accessible from localhost.
         * This value is assigned automatically as soon as the container is started.
         */
        var external: Int? = null,
    )
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
