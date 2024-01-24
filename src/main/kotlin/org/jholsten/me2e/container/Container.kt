package org.jholsten.me2e.container

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.config.utils.ContainerPortListDeserializer
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.logging.ContainerLogConsumer
import org.jholsten.me2e.container.logging.ContainerLogUtils
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.container.logging.model.ContainerLogEntryList
import org.jholsten.me2e.container.network.ContainerNetwork
import org.jholsten.me2e.container.network.mapper.ContainerNetworkMapper
import org.jholsten.me2e.container.stats.ContainerStatsConsumer
import org.jholsten.me2e.container.stats.ContainerStatsUtils
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.testcontainers.containers.ContainerState
import com.github.dockerjava.api.model.Container as DockerContainer


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
     * Result of the execution of a command inside the Docker container.
     */
    data class ExecutionResult(
        /**
         * Exit code of the command.
         */
        val exitCode: Int,
        /**
         * Output of the command on STDOUT.
         */
        val stdout: String,
        /**
         * Output of the command on STDERR.
         */
        val stderr: String,
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
     * Returns the ID of the associated Docker container.
     * @throws IllegalStateException if container is not initialized
     */
    val containerId: String
        get() {
            assertThatContainerIsInitialized()
            return dockerContainer!!.state.containerId
        }

    /**
     * Returns information about all networks that the container is connected to
     * as a map of network name and network information.
     * @throws IllegalStateException if container is not initialized
     */
    val networks: Map<String, ContainerNetwork>
        get() {
            assertThatContainerIsInitialized()
            val networkSettings = dockerContainer!!.container.networkSettings ?: return mapOf()
            return ContainerNetworkMapper.INSTANCE.toInternalDto(networkSettings.networks)
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
    internal open fun initialize(dockerContainer: DockerContainer, dockerContainerState: ContainerState) {
        this.dockerContainer = DockerContainerReference(dockerContainer, dockerContainerState)

        val dockerPorts = dockerContainer.ports.filter { it.privatePort != null }
        for (port in dockerPorts) {
            val internalPort = this.ports.findByInternalPort(port.privatePort!!)
            if (internalPort != null) {
                internalPort.external = port.publicPort
            }
        }
        ReportDataAggregator.onContainerStarted(this)
    }

    /**
     * Executes the given command inside the container, as using
     * [`docker exec`](https://docs.docker.com/engine/reference/commandline/exec/).
     * @param command Command to execute in array format. Example: `["echo", "a", "&&", "echo", "b"]`
     * @return Result of the execution.
     */
    fun execute(vararg command: String): ExecutionResult {
        assertThatContainerIsInitialized()
        val result = dockerContainer!!.state.execInContainer(*command)
        return ExecutionResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
        )
    }

    /**
     * Executes the given command inside the container as the given user, as using
     * [`docker exec -u user`](https://docs.docker.com/engine/reference/commandline/exec/).
     * @param user Username or UID to execute command with. Format is one of: `user`, `user:group`, `uid` or `uid:gid`.
     * @param command Command to execute in array format. Example: `["echo", "a", "&&", "echo", "b"]`
     * @return Result of the execution.
     */
    fun executeAsUser(user: String, vararg command: String): ExecutionResult {
        assertThatContainerIsInitialized()
        val result = dockerContainer!!.state.execInContainerWithUser(user, *command)
        return ExecutionResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr,
        )
    }

    /**
     * Returns all log output from the container from [since] until now along with their timestamps,
     * by executing `docker logs --since $since --timestamps $containerId`.
     *
     * To retrieve all log entries starting from the creation of the container, set [since] to `0`.
     * @param since Only return logs since this time, as a UNIX timestamp.
     * @throws IllegalStateException if container is not initialized
     */
    fun getLogsSince(since: Int): ContainerLogEntryList {
        return getLogs(since = since, until = null)
    }

    /**
     * Returns all log output from the container from the creation of the container until [until] along with their timestamps,
     * by executing `docker logs --until $until --timestamps $containerId`.
     *
     * To retrieve all log entries until now, set [until] to `null`.
     * @param until Only return logs before this time, as a UNIX timestamp. If set to `null`, all log entries until now are returned.
     * @throws IllegalStateException if container is not initialized
     */
    fun getLogsUntil(until: Int?): ContainerLogEntryList {
        return getLogs(since = 0, until = until)
    }

    /**
     * Returns all log output from the container from [since] until [until] along with their timestamps,
     * by executing `docker logs --since $since --until $until --timestamps $containerId`.
     *
     * To retrieve all log entries starting from the creation of the container, set [since] to `0`.
     * To retrieve all log entries until now, set [until] to `null`.
     * @param since Only return logs since this time, as a UNIX timestamp.
     * @param until Only return logs before this time, as a UNIX timestamp. If set to `null`, all log entries until now are returned.
     * @throws IllegalStateException if container is not initialized
     */
    fun getLogsBetween(since: Int, until: Int?): ContainerLogEntryList {
        return getLogs(since = since, until = until)
    }

    /**
     * Returns all log output from the container from the creation of the container until now along with their timestamps,
     * by executing `docker logs --timestamps $containerId`.
     * @throws IllegalStateException if container is not initialized
     */
    fun getLogs(): ContainerLogEntryList {
        return getLogs(since = 0, until = null)
    }

    /**
     * Attaches the given [consumer] to this container's log outputs.
     * The consumer receives all previous and all future log frames.
     * @param consumer Log consumer to be attached.
     * @throws IllegalStateException if container is not initialized
     */
    fun addLogConsumer(consumer: ContainerLogConsumer) {
        assertThatContainerIsInitialized()
        ContainerLogUtils.followOutput(dockerContainer!!.state, consumer)
    }

    /**
     * Returns current resource usage statistics of the container, by executing `docker stats --no-stream $containerId`.
     * @throws IllegalStateException if container is not initialized
     */
    fun getStats(): ContainerStatsEntry {
        assertThatContainerIsInitialized()
        return ContainerStatsUtils.getStats(dockerContainer!!.state)
    }

    /**
     * Attaches the given [consumer] to the container's resource usage statistics.
     * Docker instantiates a live data stream for the container and for each
     * statistics entry received by Docker, the consumer is notified.
     * Docker sends new resource usage information every second.
     * @param consumer Statistics consumer to be attached.
     * @throws IllegalStateException if container is not initialized
     */
    fun addStatsConsumer(consumer: ContainerStatsConsumer) {
        assertThatContainerIsInitialized()
        ContainerStatsUtils.followOutput(dockerContainer!!.state, consumer)
    }

    /**
     * Returns all log output from the container from [since] until [until] along with their timestamps,
     * by executing `docker logs --since $since --until $until --timestamps $containerId`.
     *
     * To retrieve all log entries starting from the creation of the container, set [since] to `0`.
     * To retrieve all log entries until now, set [until] to `null`.
     * @param since Only return logs since this time, as a UNIX timestamp.
     * @param until Only return logs before this time, as a UNIX timestamp. If set to `null`, all log entries until now are returned.
     * @throws IllegalStateException if container is not initialized
     */
    private fun getLogs(since: Int, until: Int?): ContainerLogEntryList {
        assertThatContainerIsInitialized()
        return ContainerLogUtils.getLogs(dockerContainer!!.state, since, until)
    }

    private fun assertThatContainerIsInitialized() {
        checkNotNull(dockerContainer) { "Docker container is not properly initialized" }
    }
}
