package org.jholsten.me2e.container

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.logging.ContainerLogConsumer
import org.jholsten.me2e.container.logging.ContainerLogUtils
import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.container.model.ContainerPortList
import org.jholsten.me2e.container.model.ContainerType
import org.jholsten.me2e.container.model.DockerContainerReference
import org.jholsten.me2e.container.model.ExecutionResult
import org.jholsten.me2e.container.network.ContainerNetwork
import org.jholsten.me2e.container.network.mapper.ContainerNetworkMapper
import org.jholsten.me2e.container.stats.ContainerStatsConsumer
import org.jholsten.me2e.container.stats.ContainerStatsUtils
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.jholsten.me2e.events.ContainerEventConsumer
import org.jholsten.me2e.events.ContainerEventsUtils
import org.jholsten.me2e.events.ContainerRestartListener
import org.jholsten.me2e.events.model.ContainerEvent
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.ContainerState
import java.time.Instant
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
    val image: String, // TODO: Check with build .

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
    private val logger = logger(this)

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
    @get:JsonIgnore
    val networks: Map<String, ContainerNetwork>
        get() {
            assertThatContainerIsInitialized()
            val networkSettings = dockerContainer!!.state.containerInfo.networkSettings ?: return mapOf()
            return ContainerNetworkMapper.INSTANCE.toInternalDto(networkSettings.networks)
        }

    /**
     * Reference to the Docker container which represents this container instance.
     * Is initialized as soon as the docker compose is started.
     */
    @JvmSynthetic
    internal var dockerContainer: DockerContainerReference? = null

    /**
     * List of registered log consumers. Are reattached whenever the container is restarted.
     */
    private val logConsumers: MutableList<ContainerLogConsumer> = mutableListOf()

    /**
     * List of registered resource usage statistics consumers. Are reattached whenever the container is restarted.
     */
    private val statsConsumers: MutableList<ContainerStatsConsumer> = mutableListOf()

    /**
     * List of registered event consumers. Are reattached whenever the container is restarted.
     */
    private val eventConsumers: MutableList<Pair<ContainerEventConsumer, List<ContainerEvent.Type>?>> = mutableListOf()

    /**
     * Initializes the container by setting the corresponding [DockerContainer] and [ContainerState] instance after the Docker
     * container was started. Maps external ports to internal container ports, initializes [ReportDataAggregator] and adds consumer
     * to the container's `restart` events.
     */
    @JvmSynthetic
    internal open fun initialize(dockerContainer: DockerContainer, state: ContainerState, environment: DockerCompose) {
        this.dockerContainer = DockerContainerReference(dockerContainer, state, environment)
        mapContainerPorts(dockerContainer)

        ReportDataAggregator.onContainerStarted(this)
        addEventConsumer(ContainerRestartListener(this), eventFilters = listOf(ContainerEvent.Type.RESTART))
    }

    /**
     * Callback function to execute when the corresponding Docker container was restarted.
     * After a restart, all existing consumers are closed and the port mappings of the container can change (see
     * [GitHub Issue #31926](https://github.com/moby/moby/issues/31926)). Therefore, all registered consumer are reattached and
     * the port mappings are updated in the internal state.
     * @param timestamp Timestamp of when the Docker container was restarted.
     */
    @JvmSynthetic
    internal open fun onRestart(timestamp: Instant) {
        assertThatContainerIsInitialized()
        logger.info("Received notification that container $name was restarted at $timestamp. Updating container info...")
        val state = dockerContainer!!.state
        dockerContainer!!.container = dockerContainer!!.environment.getDockerContainer(name)
        logConsumers.forEach { consumer -> ContainerLogUtils.followOutput(state, consumer, since = timestamp.epochSecond.toInt()) }
        statsConsumers.forEach { consumer -> ContainerStatsUtils.followOutput(state, consumer) }
        eventConsumers.forEach { (consumer, eventFilters) -> ContainerEventsUtils.followOutput(state, consumer, eventFilters) }
        mapContainerPorts(dockerContainer!!.container)
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
    fun getLogsSince(since: Int): List<ContainerLogEntry> {
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
    fun getLogsUntil(until: Int?): List<ContainerLogEntry> {
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
    fun getLogsBetween(since: Int, until: Int?): List<ContainerLogEntry> {
        return getLogs(since = since, until = until)
    }

    /**
     * Returns all log output from the container from the creation of the container until now along with their timestamps,
     * by executing `docker logs --timestamps $containerId`.
     * @throws IllegalStateException if container is not initialized
     */
    fun getLogs(): List<ContainerLogEntry> {
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
        logConsumers.add(consumer)
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
        statsConsumers.add(consumer)
        ContainerStatsUtils.followOutput(dockerContainer!!.state, consumer)
    }

    /**
     * Attaches the given [consumer] to the container's events.
     * Docker instantiates a live data stream for the container and for each event received by Docker, the consumer is notified.
     * @param consumer Event consumer to be attached.
     * @param eventFilters If provided, only events of the given types are consumed.
     */
    @JvmOverloads
    fun addEventConsumer(consumer: ContainerEventConsumer, eventFilters: List<ContainerEvent.Type>? = null) {
        assertThatContainerIsInitialized()
        eventConsumers.add(consumer to eventFilters)
        ContainerEventsUtils.followOutput(dockerContainer!!.state, consumer, eventFilters)
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
    private fun getLogs(since: Int, until: Int?): List<ContainerLogEntry> {
        assertThatContainerIsInitialized()
        return ContainerLogUtils.getLogs(dockerContainer!!.state, since, until)
    }

    private fun mapContainerPorts(dockerContainer: DockerContainer) {
        val dockerPorts = dockerContainer.ports.filter { it.privatePort != null }
        for (port in dockerPorts) {
            val internalPort = this.ports.findByInternalPort(port.privatePort!!)
            if (internalPort != null) {
                internalPort.external = port.publicPort
            }
        }
    }

    private fun assertThatContainerIsInitialized() {
        checkNotNull(dockerContainer) { "Docker container is not properly initialized" }
    }
}
