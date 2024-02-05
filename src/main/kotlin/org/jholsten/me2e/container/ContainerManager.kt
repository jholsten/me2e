package org.jholsten.me2e.container

import com.github.dockerjava.api.model.Container as DockerContainer
import org.apache.commons.lang3.RandomStringUtils
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.exception.ServiceShutdownException
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.health.exception.HealthTimeoutException
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.utils.filterValuesIsInstance
import org.jholsten.me2e.utils.logger
import java.io.File

/**
 * Manager for starting, stopping and managing all Docker containers.
 * Contains references to all [containers], [microservices] and [databases].
 */
class ContainerManager(
    /**
     * Reference to Docker-Compose file to start.
     */
    dockerComposeFile: File,

    /**
     * Configuration options for Docker.
     * Corresponds to the `docker` section of the ME2E configuration file.
     */
    private val dockerConfig: DockerConfig,

    /**
     * Self-managed containers from Docker-Compose file as map of name and [Container] instance.
     * Corresponds to the services defined in the Docker-Compose.
     */
    val containers: Map<String, Container>,
) {
    private val logger = logger(this)

    /**
     * [containers] with container type [org.jholsten.me2e.container.model.ContainerType.MICROSERVICE].
     */
    val microservices: Map<String, MicroserviceContainer> = containers.filterValuesIsInstance<String, MicroserviceContainer>()

    /**
     * [containers] with container type [org.jholsten.me2e.container.model.ContainerType.DATABASE].
     */
    val databases: Map<String, DatabaseContainer> = containers.filterValuesIsInstance<String, DatabaseContainer>()

    /**
     * Identifier of the services to start. Will be used as a prefix for the project name and therefore for all container names.
     */
    private val identifier: String = "me2e_${RandomStringUtils.randomAlphabetic(3)}".lowercase()

    /**
     * Name of the Docker-Compose project, which is composed of the [identifier] as a prefix
     * and an alphanumeric suffix, which is randomly generated each time theDocker-Compose is started.
     * @throws IllegalStateException if Docker-Compose is currently not running.
     */
    val project: String
        get() = environment.project

    /**
     * Reference to the Docker-Compose environment.
     */
    val environment = DockerCompose.Builder(identifier, dockerComposeFile, version = dockerConfig.dockerComposeVersion)
        .withLocalCompose(true)
        .withBuild(dockerConfig.buildImages)
        .withRemoveImages(dockerConfig.removeImages)
        .withRemoveVolumes(dockerConfig.removeVolumes)
        .build()

    /**
     * Starts Docker-Compose and initializes all containers. Waits until all containers for which a healthcheck is defined are healthy.
     * @throws ServiceStartupException if Docker-Compose could not be started.
     * @throws HealthTimeoutException if at least one container did not become healthy within [DockerConfig.healthTimeout] seconds.
     */
    fun start() {
        pullImages()
        environment.start()
        initializeContainers()
        environment.waitUntilHealthy(containers.keys.toList(), dockerConfig.healthTimeout)
    }

    /**
     * Restarts the containers in the environment with the given names.
     * @param containerNames Names of the containers to restart.
     * @throws IllegalArgumentException In case any of the given container names does not exist in the Docker-Compose.
     */
    fun restart(containerNames: List<String>) {
        val unknownContainers = containerNames.filter { it !in containers }
        require(unknownContainers.isEmpty()) { "Unknown container names: [${unknownContainers.joinToString(", ")}]" }
        closeDatabaseConnections(containerNames)
        environment.restartContainers(containerNames, dockerConfig.healthTimeout)
    }

    /**
     * Closes all database connections and stops the Docker-Compose environment.
     * @throws ServiceShutdownException if Docker-Compose could not be stopped.
     */
    fun stop() {
        closeDatabaseConnections(databases.keys.toList())
        environment.stop()
    }

    /**
     * Pulls images for which the pull policy is set to [DockerConfig.PullPolicy.ALWAYS].
     * For images with [DockerConfig.PullPolicy.MISSING], missing images are pulled automatically
     * on `docker compose up`.
     */
    private fun pullImages() {
        val servicesToPullAlways = containers.values
            .filter { it.pullPolicy == DockerConfig.PullPolicy.ALWAYS }
            .map { it.name }

        environment.pull(servicesToPullAlways)
    }

    /**
     * Initializes containers after the Docker-Compose is started.
     * Sets corresponding [DockerContainer] and [org.testcontainers.containers.ContainerState] instances.
     */
    private fun initializeContainers() {
        for (container in containers.values) {
            val state = environment.getContainerByServiceName(container.name)!!
            try {
                container.initialize(environment.getDockerContainer(container.name), state, environment)
            } catch (e: Exception) {
                logger.error(
                    "Unable to initialize container ${container.name}. " +
                        "It is possible that some of the functions do not work properly.", e
                )
            }
        }
    }

    /**
     * Before the database containers are stopped, their connections need to be stopped, otherwise this may lead to a memory leak.
     * This method closes all existing connections to all SQL and No-SQL databases.
     */
    private fun closeDatabaseConnections(servicesToClose: List<String>) {
        val databaseConnections = databases.values.filter { it.name in servicesToClose }.mapNotNull { it.connection }
        for (connection in databaseConnections) {
            connection.close()
        }
    }
}
