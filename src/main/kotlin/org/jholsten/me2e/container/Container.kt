package org.jholsten.me2e.container

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.dockerjava.api.exception.DockerClientException
import com.github.dockerjava.api.exception.NotFoundException
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
import org.jholsten.me2e.container.events.ContainerEventConsumer
import org.jholsten.me2e.container.events.ContainerEventsUtils
import org.jholsten.me2e.container.events.ContainerRestartListener
import org.jholsten.me2e.container.events.model.ContainerEvent
import org.jholsten.me2e.container.exception.DockerException
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.utils.logger
import org.testcontainers.containers.ContainerState
import org.testcontainers.utility.MountableFile
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant
import com.github.dockerjava.api.model.Container as DockerContainer


/**
 * Representation of one Docker container.
 * This may be a microservice, a database or any other kind of supporting service.
 * @see MicroserviceContainer
 * @see DatabaseContainer
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
open class Container internal constructor(
    /**
     * Unique name of this container.
     */
    val name: String,

    /**
     * Type of this container.
     * Corresponds to the value of the label `org.jholsten.me2e.container-type` in the Docker-Compose.
     */
    val type: ContainerType = ContainerType.MISC,

    /**
     * Image to start the container from.
     * Corresponds to the value given for the `image` keyword in Docker-Compose.
     */
    val image: String?,

    /**
     * Environment variables for this container.
     * Corresponds to the values given in the `environment` section of the Docker-Compose.
     */
    val environment: Map<String, String>? = null,

    /**
     * Ports that should be exposed to localhost.
     * Corresponds to the `ports` section of the Docker-Compose.
     */
    val ports: ContainerPortList = ContainerPortList(),

    /**
     * Pull policy for this Docker container.
     * If not overwritten in the label `org.jholsten.me2e.pull-policy` for this container, the global
     * pull policy [org.jholsten.me2e.config.model.DockerConfig.pullPolicy] is used.
     * @see org.jholsten.me2e.config.model.DockerConfig.pullPolicy
     */
    val pullPolicy: DockerConfig.PullPolicy = DockerConfig.PullPolicy.MISSING,
) {
    private val logger = logger<Container>()


    /**
     * Whether there is a healthcheck defined for this container either inside the Docker image
     * or in the Docker-Compose file.
     */
    var hasHealthcheck: Boolean = false

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
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    val containerId: String
        get() {
            assertThatContainerIsInitialized()
            return dockerContainer!!.state.containerId
        }

    /**
     * Returns information about all networks that the container is connected to
     * as a map of network name and network information.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
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
     * Executes the given command inside the container in the container's working directory,
     * as using [`docker exec`](https://docs.docker.com/engine/reference/commandline/exec/).
     * Note that the exit code of the result is not checked, i.e. the execution may have been unsuccessful.
     * @param command Command to execute in array format. Example: `["echo", "a", "&&", "echo", "b"]`
     * @return Result of the execution.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/exec/">Docker Documentation</a>
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
     * Executes the given command inside the container as the given user in the container's working directory, as using
     * [`docker exec -u user`](https://docs.docker.com/engine/reference/commandline/exec/).
     * Note that the exit code of the result is not checked, i.e. the execution may have been unsuccessful.
     * @param user Username or UID to execute command with. Format is one of: `user`, `user:group`, `uid` or `uid:gid`.
     * @param command Command to execute in array format. Example: `["echo", "a", "&&", "echo", "b"]`
     * @return Result of the execution.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/exec/">Docker Documentation</a>
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
     * Copies the file or directory from this host at the given path located in the `resources` folder to the given
     * path inside the container.
     * @param sourcePath Path to the file or directory in `resources` folder to be copied.
     * @param containerPath Absolute path to the destination file or directory inside the container. Note that this is
     * always considered as an absolute path inside the container, independent of the container's working directory.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     * @throws java.io.FileNotFoundException if file at [sourcePath] does not exist.
     * @throws DockerException if file or directory could not be copied.
     */
    fun copyResourceToContainer(sourcePath: String, containerPath: String) {
        val sourceFile = try {
            MountableFile.forClasspathResource(sourcePath)
        } catch (e: IllegalArgumentException) {
            throw FileNotFoundException(e.message)
        }
        copyFileToContainer(sourceFile, containerPath)
    }

    /**
     * Copies the file or directory from this host at the given path to the given path inside the container.
     * @param sourcePath Absolute path to the file or directory on this host to be copied.
     * @param containerPath Absolute path to the destination file or directory inside the container. Note that this is
     * always considered as an absolute path inside the container, independent of the container's working directory.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     * @throws java.io.FileNotFoundException if file at [sourcePath] does not exist.
     * @throws DockerException if file or directory could not be copied.
     */
    fun copyFileToContainer(sourcePath: String, containerPath: String) {
        if (!File(sourcePath).exists()) {
            throw FileNotFoundException("File or directory with absolute path '$sourcePath' could not be found on the host.")
        }
        copyFileToContainer(MountableFile.forHostPath(sourcePath), containerPath)
    }

    /**
     * Copies the single file inside the container at the given path to the given path on this host.
     * Note that this method only supports copying single files and not directories.
     * @param containerPath Absolute path to the file to be copied inside the container. Note that this is always
     * considered as an absolute path inside the container, independent of the container's working directory.
     * @param destinationPath Absolute path to the destination file on this host.
     * @return File copied from the container.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     * @throws java.io.FileNotFoundException if file at [containerPath] does not exist.
     * @throws DockerException if file could not be copied.
     */
    fun copyFileFromContainer(containerPath: String, destinationPath: String): File {
        assertThatContainerIsInitialized()
        logger.debug("Copying file '$containerPath' from container $name to host at path '$destinationPath'...")
        try {
            val destinationFile = File(destinationPath)
            destinationFile.parentFile?.mkdirs()
            dockerContainer!!.state.copyFileFromContainer(containerPath, destinationPath)
            return destinationFile
        } catch (e: NotFoundException) {
            throw FileNotFoundException("File '$containerPath' could not be found in container $name.")
        } catch (e: DockerClientException) {
            throw DockerException("File '$containerPath' could not be copied from container $name.", e)
        }
    }

    /**
     * Returns all log output from the container from [since] until now along with their timestamps,
     * by executing `docker logs --since $since --timestamps $containerId`.
     *
     * To retrieve all log entries starting from the creation of the container, set [since] to `0`.
     * @param since Only return logs since this time, as a UNIX timestamp.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
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
     * @throws IllegalStateException if container is not initialized, i.e. not started.
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
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    fun getLogsBetween(since: Int, until: Int?): List<ContainerLogEntry> {
        return getLogs(since = since, until = until)
    }

    /**
     * Returns all log output from the container from the creation of the container until now along with their timestamps,
     * by executing `docker logs --timestamps $containerId`.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    fun getLogs(): List<ContainerLogEntry> {
        return getLogs(since = 0, until = null)
    }

    /**
     * Attaches the given [consumer] to this container's log outputs.
     * The consumer receives all previous and all future log frames.
     * @param consumer Log consumer to be attached.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    fun addLogConsumer(consumer: ContainerLogConsumer) {
        assertThatContainerIsInitialized()
        logConsumers.add(consumer)
        ContainerLogUtils.followOutput(dockerContainer!!.state, consumer)
    }

    /**
     * Returns current resource usage statistics of the container, by executing `docker stats --no-stream $containerId`.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    fun getStats(): ContainerStatsEntry {
        assertThatContainerIsInitialized()
        return ContainerStatsUtils.getStats(dockerContainer!!.state)
    }

    /**
     * Attaches the given [consumer] to the container's resource usage statistics.
     * Docker instantiates a live data stream for the container and sends new resource usage information every second.
     * For each statistics entry received by Docker, the consumer is notified.
     * @param consumer Statistics consumer to be attached.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
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
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    @JvmOverloads
    fun addEventConsumer(consumer: ContainerEventConsumer, eventFilters: List<ContainerEvent.Type>? = null) {
        assertThatContainerIsInitialized()
        eventConsumers.add(consumer to eventFilters)
        ContainerEventsUtils.followOutput(dockerContainer!!.state, consumer, eventFilters)
    }

    /**
     * Initializes the container by setting the corresponding [DockerContainer] and [ContainerState] instance after the Docker
     * container was started. Maps internal ports to external container ports, initializes [ReportDataAggregator] and adds consumer
     * to the container's `restart` events.
     * @param dockerContainer Reference to the corresponding Docker container.
     * @param state Reference to the corresponding Docker container state. Required to interact with the container.
     * @param environment Docker-Compose environment which the container is part of.
     */
    @JvmSynthetic
    internal open fun initializeOnContainerStarted(dockerContainer: DockerContainer, state: ContainerState, environment: DockerCompose) {
        this.dockerContainer = DockerContainerReference(dockerContainer, state, environment)
        this.hasHealthcheck = environment.hasHealthcheck(name)
        mapContainerPorts(dockerContainer)

        ReportDataAggregator.onContainerStarted(this)
        addEventConsumer(ContainerRestartListener(this), eventFilters = listOf(ContainerEvent.Type.RESTART))
    }

    /**
     * Callback function to execute when all containers of the environment are healthy.
     */
    @JvmSynthetic
    internal open fun initializeOnContainerHealthy() {
    }

    /**
     * Callback function to execute when the corresponding Docker container was restarted.
     * After a restart, all existing consumers are closed and the port mappings of the container can change (see
     * [GitHub Issue #31926](https://github.com/moby/moby/issues/31926)). Therefore, all registered consumers are
     * reattached and the port mappings are updated in the internal state.
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
     * Returns all log output from the container from [since] until [until] along with their timestamps,
     * by executing `docker logs --since $since --until $until --timestamps $containerId`.
     *
     * To retrieve all log entries starting from the creation of the container, set [since] to `0`.
     * To retrieve all log entries until now, set [until] to `null`.
     * @param since Only return logs since this time, as a UNIX timestamp.
     * @param until Only return logs before this time, as a UNIX timestamp. If set to `null`, all log entries until now are returned.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     */
    private fun getLogs(since: Int, until: Int?): List<ContainerLogEntry> {
        assertThatContainerIsInitialized()
        return ContainerLogUtils.getLogs(dockerContainer!!.state, since, until)
    }

    /**
     * Maps all internal ports defined for the Docker container (i.e. all entries in the `ports` section of the Docker-Compose) to
     * the external, publicly accessible port. If the external port is specified in the port binding and is therefore fixed, this
     * specified value is used. However, if no external port is defined in the Docker-Compose, Docker selects a random external port.
     * The port bindings are updated in the [ports] of this container instance.
     */
    private fun mapContainerPorts(dockerContainer: DockerContainer) {
        val dockerPorts = dockerContainer.ports.filter { it.privatePort != null }
        for (port in dockerPorts) {
            val internalPort = this.ports.findByInternalPort(port.privatePort!!)
            if (internalPort != null) {
                internalPort.external = port.publicPort
            }
        }
    }

    /**
     * Copies the file or directory from this host to the given path inside the container.
     * @param sourceFile File or directory on this host to be copied.
     * @param containerPath Absolute path to the destination file or directory inside the container. Note that this is
     * always considered as an absolute path inside the container, independent of the container's working directory.
     * @throws IllegalStateException if container is not initialized, i.e. not started.
     * @throws java.io.FileNotFoundException if file [sourceFile] does not exist.
     * @throws DockerException if file or directory could not be copied.
     */
    private fun copyFileToContainer(sourceFile: MountableFile, containerPath: String) {
        assertThatContainerIsInitialized()
        logger.debug("Copying file or directory '${sourceFile.resolvedPath}' to container $name at path '$containerPath'...")
        try {
            dockerContainer!!.state.copyFileToContainer(sourceFile, containerPath)
        } catch (e: DockerClientException) {
            throw DockerException("File or directory '${sourceFile.resolvedPath}' could not be copied to container $name.", e)
        }
    }

    private fun assertThatContainerIsInitialized() {
        checkNotNull(dockerContainer) { "Docker container is not properly initialized. Did you start the container first?" }
    }
}
