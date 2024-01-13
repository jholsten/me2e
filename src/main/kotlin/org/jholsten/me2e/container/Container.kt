package org.jholsten.me2e.container

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.model.DockerConfig
import com.github.dockerjava.api.model.Container as DockerContainer
import org.jholsten.me2e.config.utils.ContainerPortListDeserializer
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import org.testcontainers.containers.ContainerState


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
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = MicroserviceContainer::class, name = "MICROSERVICE"),
        JsonSubTypes.Type(value = DatabaseContainer::class, name = "DATABASE"),
    ]
)
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
     * Ports that should be exposed to localhost.
     */
    val ports: ContainerPortList = ContainerPortList(),

    /**
     * Whether there is a healthcheck defined for this container in the Docker-Compose file.
     */
    val hasHealthcheck: Boolean = false,

    /**
     * Pull policy for this Docker container.
     */
    val pullPolicy: DockerConfig.PullPolicy = DockerConfig.PullPolicy.MISSING,
) {
    @JsonDeserialize(using = ContainerPortListDeserializer::class)
    class ContainerPortList(ports: List<ContainerPort> = listOf()) : ArrayList<ContainerPort>(ports) {
        /**
         * Returns the first [ContainerPort] instance for which the internal port is equal to the given [port]
         * or `null`, if no such instance exists in this list.
         */
        fun findByInternalPort(port: Int): ContainerPort? {
            return this.firstOrNull { it.internal == port }
        }

        /**
         * Returns the first [ContainerPort] instance for which an external port is set or `null`, if no such
         * instance exists in this list.
         */
        fun findFirstExposed(): ContainerPort? {
            return this.firstOrNull { it.external != null }
        }
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
     * Wrapper for the reference to the corresponding [DockerContainer] and [ContainerState].
     */
    class DockerContainerReference(
        /**
         * Container which contains static and dynamic information about the Docker container.
         */
        val container: DockerContainer,
        /**
         * State which enables to execute commands in the Docker container.
         */
        val state: ContainerState,
    )

    /**
     * Returns whether the container is currently up and running.
     */
    val isRunning: Boolean
        get() = dockerContainer?.state?.isRunning == true

    /**
     * Returns whether the container is currently healthy.
     * Always returns false if there is no healthcheck specified for this container.
     */
    val isHealthy: Boolean
        get() {
            return try {
                dockerContainer?.state?.isHealthy == true
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Reference to the Docker container which represents this container instance.
     * Is initialized as soon as the docker compose is started.
     */
    private var dockerContainer: DockerContainerReference? = null

    /**
     * Initializes the container by setting the corresponding [DockerContainer] and [ContainerState] instance
     * after the Docker container was started.
     * Maps external ports to internal container ports.
     */
    @JvmSynthetic
    internal open fun initialize(dockerConfig: DockerConfig, dockerContainer: DockerContainer, dockerContainerState: ContainerState) {
        this.dockerContainer = DockerContainerReference(dockerContainer, dockerContainerState)

        val dockerPorts = dockerContainer.ports.filter { it.privatePort != null }
        for (port in dockerPorts) {
            val internalPort = this.ports.findByInternalPort(port.privatePort!!)
            if (internalPort != null) {
                internalPort.external = port.publicPort
            }
        }
    }

    /**
     * Executes the given command inside the container.
     */
    fun execute() {
        // TODO
    }

    fun getLogs(): String {
        checkNotNull(dockerContainer) { "Docker container is not properly initialized" }
        return dockerContainer!!.state.logs
    }
}
