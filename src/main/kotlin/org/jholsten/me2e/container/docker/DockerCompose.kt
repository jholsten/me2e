package org.jholsten.me2e.container.docker

import org.jholsten.me2e.config.model.DockerConfig.DockerComposeRemoveImagesStrategy
import com.github.dockerjava.api.model.Container as DockerContainer
import org.jholsten.me2e.container.exception.DockerException
import org.jholsten.me2e.container.exception.ServiceShutdownException
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.health.ContainerWaitStrategyTarget
import org.jholsten.me2e.container.health.exception.HealthTimeoutException
import org.jholsten.me2e.utils.ResettableLazy
import org.jholsten.me2e.utils.logger
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.wait.strategy.WaitStrategy
import org.testcontainers.shaded.com.github.dockerjava.core.LocalDirectorySSLConfig
import org.testcontainers.shaded.com.google.common.base.Splitter
import org.testcontainers.shaded.org.zeroturnaround.exec.InvalidExitValueException
import org.testcontainers.shaded.org.zeroturnaround.exec.ProcessExecutor
import org.testcontainers.shaded.org.zeroturnaround.exec.stream.slf4j.Slf4jStream
import org.testcontainers.utility.CommandLine
import org.testcontainers.utility.DockerLoggerFactory
import org.testcontainers.containers.DockerComposeContainer as DockerComposeV1
import org.testcontainers.containers.ComposeContainer as DockerComposeV2
import java.io.File
import java.time.Duration

/**
 * Service for instantiating and interacting with a Docker-Compose environment using either Docker-Compose version v1 or v2.
 * To instantiate the environment, use the [Builder].
 *
 * This service is a custom wrapper for testcontainers Docker-Compose classes [DockerComposeV1] and [DockerComposeV2].
 * Since testcontainers uses two different classes for version 1 and 2, which do not have a common supertype, it is otherwise
 * difficult to handle instances of both classes. Therefore, this class provides a common interface for both versions.
 */
@Suppress("DEPRECATION")
class DockerCompose private constructor(
    /**
     * Unique identifier of the Docker-Compose project.
     * Is used as a prefix for the [project] name.
     */
    private val identifier: String,

    /**
     * Reference to the Docker-Compose file to use.
     */
    private val file: File,

    /**
     * Docker-Compose version to use for executing commands.
     */
    private val version: DockerComposeVersion,

    /**
     * Lazy reference to testcontainers implementation for Docker-Compose version 1.
     */
    v1: Lazy<KDockerComposeV1>,

    /**
     * Lazy reference to testcontainers implementation for Docker-Compose version 2.
     */
    v2: Lazy<DockerComposeV2>,
) {
    private val logger = logger<DockerCompose>()

    /**
     * Name of the Docker-Compose project, which is composed of the [identifier] as a prefix
     * and an alphanumeric suffix, which is randomly generated each time the Docker-Compose is started.
     * Is only set after the Docker-Compose is started and reset to `null` after it is stopped.
     */
    private var _project: String? = null

    /**
     * Name of the docker compose project, which is composed of the [identifier] as a prefix
     * and an alphanumeric suffix, which is randomly generated each time the docker compose is started.
     * @throws IllegalStateException if docker compose is currently not running.
     */
    val project: String
        get() {
            checkNotNull(_project) {
                "Docker-Compose is currently not running. Since the project name is " +
                    "randomly generated on each start, the name cannot be retrieved."
            }
            return _project!!
        }

    /**
     * Lazy reference to a map of service name and [DockerContainer] instance for all services defined in the Docker-Compose [file].
     * Is reset after the Docker-Compose is started, when any service is restarted or when the Docker-Compose is stopped.
     */
    private val _dockerContainers: ResettableLazy<Map<String, DockerContainer>> = ResettableLazy {
        getDockerContainers()
    }

    /**
     * Map of service name and [DockerContainer] instance for all services defined in the Docker-Compose [file].
     * As the port mapping of containers can change during a restart, this map is retrieved directly from Docker each time during
     * a restart in order to obtain the up-to-date information. Subsequently, the data is stored in the internal memory is accessed
     * which may be outdated, especially in terms of the container's statuses.
     * @throws IllegalStateException if docker compose is currently not running.
     */
    @get:JvmSynthetic
    internal val dockerContainers: Map<String, DockerContainer>
        get() {
            if (isRestarting) {
                _dockerContainers.reset()
            }
            return _dockerContainers.value
        }

    /**
     * When containers are restarted, they are informed asynchronously via a corresponding [org.jholsten.me2e.container.events.model.ContainerEvent].
     * As the port mappings may have changed, the restarted containers retrieve their current information from the [dockerContainers].
     * Since the process is asynchronous and the containers are restarted one after the other, the current information must be retrieved
     * from Docker each time during the restart process. This procedure is controlled with this variable, which is set to `true` before
     * a restart and to `false` after all containers have finished restarting.
     */
    private var isRestarting: Boolean = false

    /**
     * Kotlin wrapper for [DockerComposeV1] to avoid typing issue.
     * @see <a href="https://github.com/testcontainers/testcontainers-java/issues/1010">GitHub Issue #1010</a>
     */
    internal class KDockerComposeV1(identifier: String, file: File) : DockerComposeV1<KDockerComposeV1>(identifier, file)

    /**
     * Lazy reference to testcontainers implementation for Docker-Compose version 1.
     */
    private val v1: KDockerComposeV1 by v1

    /**
     * Lazy reference to testcontainers implementation for Docker-Compose version 2.
     */
    private val v2: DockerComposeV2 by v2

    /**
     * Component to execute custom Docker-Compose commands locally.
     */
    private val local: Local by lazy { Local() }

    /**
     * Returns [ContainerState] instance for the service with the given name.
     * @see DockerComposeV1.getContainerByServiceName
     * @see DockerComposeV2.getContainerByServiceName
     */
    @JvmSynthetic
    internal fun getContainerByServiceName(serviceName: String): ContainerState? {
        return when (this.version) {
            DockerComposeVersion.V1 -> v1.getContainerByServiceName(serviceName).orElse(null)
            DockerComposeVersion.V2 -> v2.getContainerByServiceName(serviceName).orElse(null)
        }
    }

    /**
     * Starts the Docker-Compose container. Retrieves the Docker-Compose's project name from the container's labels.
     * @see DockerComposeV1.start
     * @see DockerComposeV2.start
     * @throws ServiceStartupException if Docker-Compose could not be started.
     */
    fun start() {
        try {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.start()
                DockerComposeVersion.V2 -> v2.start()
            }
        } catch (e: Exception) {
            throw ServiceStartupException("Unable to start Docker-Compose: ${e.message}")
        }
        this._dockerContainers.reset()
        if (dockerContainers.isNotEmpty()) {
            this._project = dockerContainers.values.first().labels["com.docker.compose.project"]
        }
    }

    /**
     * Restarts the services with the given names and waits for them to become healthy within the specified [healthTimeout].
     * @param servicesToRestart Names of the services to restart.
     * @param healthTimeout Maximum number of seconds to wait until the services become healthy.
     * @throws DockerException in case of errors.
     * @throws HealthTimeoutException if at least one of the services did not become healthy within [healthTimeout] seconds.
     */
    fun restartContainers(servicesToRestart: List<String>, healthTimeout: Long) {
        if (servicesToRestart.isNotEmpty()) {
            isRestarting = true
            logger.info("Restarting services: [${servicesToRestart.joinToString(", ")}]...")
            execute("restart ${servicesToRestart.joinToString(" ")}")
            this._dockerContainers.reset()
            isRestarting = false
            waitUntilHealthy(servicesToRestart, healthTimeout)
        }
    }

    /**
     * Stops the Docker-Compose container.
     * @see DockerComposeV1.stop
     * @see DockerComposeV2.stop
     * @throws ServiceShutdownException if Docker-Compose could not be stopped.
     */
    fun stop() {
        try {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.stop()
                DockerComposeVersion.V2 -> v2.stop()
            }
        } catch (e: Exception) {
            throw ServiceShutdownException("Unable to stop Docker-Compose: ${e.message}")
        }
        this._dockerContainers.reset()
        this._project = null
    }

    /**
     * Waits for up to [timeout] seconds until the services with the given names are healthy.
     * Services for which no health check is defined in the Docker-Compose are ignored.
     * @throws HealthTimeoutException if at least one of the containers did not become healthy within [timeout] seconds.
     */
    fun waitUntilHealthy(serviceNames: List<String>, timeout: Long) {
        val servicesWithHealthcheck = serviceNames.filter { hasHealthcheck(it) }
        logger.info("Waiting at most $timeout seconds for ${servicesWithHealthcheck.size} services to become healthy...")
        servicesWithHealthcheck.forEach { waitUntilHealthy(it, timeout) }
        logger.info("Services [${servicesWithHealthcheck.joinToString(", ")}] are healthy.")
    }

    /**
     * Pulls images for services with the given names.
     * @param servicesToPull Names of the services for which images are to be pulled.
     * @throws DockerException if at least one of the images could not be pulled.
     */
    fun pull(servicesToPull: List<String>) {
        if (servicesToPull.isNotEmpty()) {
            logger.info("Pulling images for services: [${servicesToPull.joinToString(", ")}]...")
            execute("pull ${servicesToPull.joinToString(" ")}")
        }
    }

    /**
     * Executes the given Docker-Compose command locally.
     * Redirects output and error to the [logger].
     * @throws DockerException in case of errors.
     */
    fun execute(command: String) {
        local.execute(command)
    }

    /**
     * Returns reference to the [DockerContainer] instance of the service with the given name.
     * May contain outdated information and is only updated when the Docker-Compose is started or any service is restarted.
     * @throws IllegalStateException if Docker-Compose is currently not running or the service does not exist.
     */
    @JvmSynthetic
    internal fun getDockerContainer(serviceName: String): DockerContainer {
        return dockerContainers[serviceName] ?: throw IllegalStateException("Unknown service $serviceName.")
    }

    /**
     * Custom implementation to execute Docker-Compose commands locally.
     * @see org.testcontainers.containers.LocalDockerCompose
     */
    inner class Local {
        private val logger = DockerLoggerFactory.getLogger(composeExecutable)

        /**
         * Executable to use for the Docker-Compose commands.
         */
        private val composeExecutable: String
            get() = when (version) {
                DockerComposeVersion.V1 -> DockerComposeV1.COMPOSE_EXECUTABLE
                DockerComposeVersion.V2 -> DockerComposeV2.COMPOSE_EXECUTABLE
            }

        /**
         * Directory where command is to be executed.
         * Is set to the directory of the Docker-Compose file.
         */
        private val pwd = file.absoluteFile.parentFile.absoluteFile

        /**
         * Docker network configuration.
         */
        private val transportConfig = DockerClientFactory.instance().transportConfig

        /**
         * Environment variables to set for executing the command.
         * Required by testcontainers.
         */
        private val environment = mutableMapOf(
            "DOCKER_HOST" to transportConfig.dockerHost.toString(),
            "COMPOSE_FILE" to file.absolutePath,
        )

        init {
            if (!CommandLine.executableExists(composeExecutable)) {
                throw DockerException("Local Docker Compose not found. Is $composeExecutable on the PATH?")
            }
            val sslConfig = transportConfig.sslConfig
            if (sslConfig != null) {
                if (sslConfig is LocalDirectorySSLConfig) {
                    environment["DOCKER_CERT_PATH"] = sslConfig.dockerCertPath
                    environment["DOCKER_TLS_VERIFY"] = "true"
                } else {
                    logger.warn("Couldn't set DOCKER_CERT_PATH. `sslConfig` is present but it's not LocalDirectorySSLConfig.")
                }
            }
        }

        /**
         * Executes the given Docker-Compose command and redirects output and error to the [logger].
         *
         * Examples:
         *
         * | Version     | Provided Command      | Executed Command                      |
         * | ----------- | --------------------- | ------------------------------------- |
         * | V1          | `pull`                | `docker-compose pull`                 |
         * | V2          | `pull`                | `docker compose pull`                 |
         * | V2          | `-p "Project" up -d`  | `docker compose -p "Project" up -d`   |
         *
         * @throws DockerException in case of errors.
         */
        fun execute(command: String) {
            val environment = this.environment + mapOf("COMPOSE_PROJECT_NAME" to _project)

            val baseCommand = when (version) {
                DockerComposeVersion.V1 -> DockerComposeV1.COMPOSE_EXECUTABLE
                DockerComposeVersion.V2 -> DockerComposeV2.COMPOSE_EXECUTABLE + " compose"
            }

            val cmd = Splitter
                .onPattern(" ")
                .omitEmptyStrings()
                .splitToList("$baseCommand $command")

            logger.debug("Running Docker-Compose command {}...", cmd)
            try {
                ProcessExecutor()
                    .command(cmd)
                    .redirectOutput(Slf4jStream.of(logger).asInfo())
                    .redirectError(Slf4jStream.of(logger).asInfo())
                    .environment(environment)
                    .directory(pwd)
                    .exitValueNormal()
                    .executeNoTimeout()
            } catch (e: InvalidExitValueException) {
                throw DockerException("Running Docker-Compose command $cmd failed: ${e.message}", e)
            }
        }
    }

    /**
     * Builder to instantiate a [DockerCompose] instance.
     */
    class Builder(
        private val identifier: String,
        private val file: File,
        private val version: DockerComposeVersion,
    ) {
        /**
         * Lazy reference to testcontainers implementation for Docker-Compose version 1.
         */
        private val v1: Lazy<KDockerComposeV1> = lazy {
            KDockerComposeV1(identifier, file)
        }

        /**
         * Lazy reference to testcontainers implementation for Docker-Compose version 2.
         */
        private val v2: Lazy<DockerComposeV2> = lazy {
            DockerComposeV2(identifier, file)
        }

        /**
         * Whether to use a local Docker-Compose binary instead of a container.
         * @see DockerComposeV1.withLocalCompose
         * @see DockerComposeV2.withLocalCompose
         */
        fun withLocalCompose(localCompose: Boolean) = apply {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.value.withLocalCompose(localCompose)
                DockerComposeVersion.V2 -> v2.value.withLocalCompose(localCompose)
            }
        }

        /**
         * Whether to always build images before starting containers.
         * @see DockerComposeV1.withBuild
         * @see DockerComposeV2.withBuild
         */
        fun withBuild(build: Boolean) = apply {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.value.withBuild(build)
                DockerComposeVersion.V2 -> v2.value.withBuild(build)
            }
        }

        /**
         * Whether to remove images after containers shut down.
         * @see DockerComposeV1.withRemoveImages
         * @see DockerComposeV2.withRemoveImages
         */
        fun withRemoveImages(removeImages: DockerComposeRemoveImagesStrategy) = apply {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.value.withRemoveImages(removeImages.toV1())
                DockerComposeVersion.V2 -> v2.value.withRemoveImages(removeImages.toV2())
            }
        }

        /**
         * Whether to remove volumes after containers shut down.
         * @see DockerComposeV1.withRemoveVolumes
         * @see DockerComposeV2.withRemoveVolumes
         */
        fun withRemoveVolumes(removeVolumes: Boolean) = apply {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.value.withRemoveVolumes(removeVolumes)
                DockerComposeVersion.V2 -> v2.value.withRemoveVolumes(removeVolumes)
            }
        }

        /**
         * Specify the [WaitStrategy] to use to determine if the container is ready.
         * @param serviceName the name of the service to wait for
         * @param waitStrategy the wait strategy to use
         * @see DockerComposeV1.waitingFor
         * @see DockerComposeV2.waitingFor
         */
        fun waitingFor(serviceName: String, waitStrategy: WaitStrategy) = apply {
            when (this.version) {
                DockerComposeVersion.V1 -> v1.value.waitingFor(serviceName, waitStrategy)
                DockerComposeVersion.V2 -> v2.value.waitingFor(serviceName, waitStrategy)
            }
        }

        /**
         * Builds [DockerCompose] instance with the properties set.
         */
        fun build(): DockerCompose {
            return DockerCompose(
                identifier = identifier,
                file = file,
                version = version,
                v1 = v1,
                v2 = v2,
            )
        }

        /**
         * Maps [DockerComposeRemoveImagesStrategy] to the equivalent for [DockerComposeV1].
         */
        private fun DockerComposeRemoveImagesStrategy.toV1(): DockerComposeV1.RemoveImages? {
            return when (this) {
                DockerComposeRemoveImagesStrategy.NONE -> null
                DockerComposeRemoveImagesStrategy.ALL -> DockerComposeV1.RemoveImages.ALL
                DockerComposeRemoveImagesStrategy.LOCAL -> DockerComposeV1.RemoveImages.LOCAL
            }
        }

        /**
         * Maps [DockerComposeRemoveImagesStrategy] to the equivalent for [DockerComposeV2].
         */
        private fun DockerComposeRemoveImagesStrategy.toV2(): DockerComposeV2.RemoveImages? {
            return when (this) {
                DockerComposeRemoveImagesStrategy.NONE -> null
                DockerComposeRemoveImagesStrategy.ALL -> DockerComposeV2.RemoveImages.ALL
                DockerComposeRemoveImagesStrategy.LOCAL -> DockerComposeV2.RemoveImages.LOCAL
            }
        }
    }

    /**
     * Waits at most [timeout] seconds until the service with the given name becomes healthy.
     * @throws HealthTimeoutException if the service did not become healthy within [timeout] seconds.
     */
    private fun waitUntilHealthy(serviceName: String, timeout: Long) {
        val waitStrategy = Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(timeout))
        try {
            logger.debug("Waiting for service $serviceName to become healthy...")
            waitStrategy.waitUntilReady(ContainerWaitStrategyTarget(getDockerContainer(serviceName).id))
            logger.debug("Service $serviceName is healthy.")
        } catch (e: ContainerLaunchException) {
            throw HealthTimeoutException("Timed out waiting for container $serviceName to become healthy.")
        }
    }

    /**
     * Returns whether there is a healthcheck defined for the service with the given name.
     */
    private fun hasHealthcheck(serviceName: String): Boolean {
        val containerId = getDockerContainer(serviceName).id
        val containerInfo = DockerClientFactory.lazyClient().inspectContainerCmd(containerId).exec()
        return containerInfo.state.health != null
    }

    /**
     * Returns map of service name and [DockerContainer] instance for all services in the Docker-Compose file,
     * by executing `docker ps`. Includes only containers which are part of the Docker-Compose [file], i.e. whose
     * container name starts with the [identifier].
     */
    private fun getDockerContainers(): Map<String, DockerContainer> {
        return DockerClientFactory.lazyClient()
            .listContainersCmd()
            .withShowAll(true)
            .exec()
            .filter { container -> container.names.any { name -> name.startsWith("/$identifier") } }
            .associateBy { container -> container.labels["com.docker.compose.service"]!! }
    }
}
