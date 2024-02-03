package org.jholsten.me2e.container

import com.github.dockerjava.api.model.Container as DockerContainer
import org.apache.commons.lang3.RandomStringUtils
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.database.DatabaseContainer
import org.jholsten.me2e.container.database.connection.MongoDBConnection
import org.jholsten.me2e.container.database.connection.SQLDatabaseConnection
import org.jholsten.me2e.container.docker.DockerCompose
import org.jholsten.me2e.container.exception.ServiceShutdownException
import org.jholsten.me2e.container.exception.ServiceStartupException
import org.jholsten.me2e.container.exception.ServiceNotHealthyException
import org.jholsten.me2e.container.microservice.MicroserviceContainer
import org.jholsten.me2e.utils.filterValuesIsInstance
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

/**
 * Manager for starting and stopping all Docker containers.
 */
class ContainerManager(
    /**
     * Reference to Docker-Compose file to start.
     */
    dockerComposeFile: File,

    /**
     * Configuration options for Docker.
     */
    private val dockerConfig: DockerConfig,

    /**
     * Self-managed containers from Docker-Compose file.
     */
    val containers: Map<String, Container>,
) {
    /**
     * [containers] with container type [org.jholsten.me2e.container.model.ContainerType.MICROSERVICE].
     */
    val microservices: Map<String, MicroserviceContainer> = containers.filterValuesIsInstance<String, MicroserviceContainer>()

    /**
     * [containers] with container type [org.jholsten.me2e.container.model.ContainerType.DATABASE].
     */
    val databases: Map<String, DatabaseContainer> = containers.filterValuesIsInstance<String, DatabaseContainer>()

    /**
     * Identifier of the services to start. Will be used as a prefix for the project and therefore for all container names.
     */
    private val identifier: String = "me2e_${RandomStringUtils.randomAlphabetic(3)}".lowercase()

    /**
     * Name of the docker compose project, which is composed of the [identifier] as a prefix
     * and an alphanumeric suffix, which is randomly generated each time the docker compose is started.
     * @throws IllegalStateException if docker compose is currently not running.
     */
    val project: String
        get() = environment.project

    /**
     * Reference to the Docker-Compose Container.
     */
    val environment = DockerCompose(identifier, dockerComposeFile, version = dockerConfig.dockerComposeVersion)
        .withLocalCompose(true)
        .withBuild(dockerConfig.buildImages)
        .withRemoveImages(dockerConfig.removeImages)
        .withRemoveVolumes(dockerConfig.removeVolumes)

    /**
     * Starts Docker-Compose and initializes all containers. Waits until all containers for which a healthcheck is defined are healthy.
     * @throws ServiceStartupException if Docker-Compose could not be started.
     * @throws ServiceNotHealthyException if at least one container did not become healthy within the specified timeout.
     */
    fun start() {
        pullImages()
        registerHealthChecks()
        startDockerCompose()
        initializeContainers()
    }

    /**
     * Restarts the containers in the environment with the given names.
     * @param containerNames Names of the containers to restart.
     * @throws IllegalArgumentException In case any of the given container names does not exist in the Docker-Compose.
     */
    fun restart(containerNames: List<String>) {
        val unknownContainers = containerNames.filter { it !in containers }
        require(unknownContainers.isEmpty()) { "Unknown container names: [${unknownContainers.joinToString(", ")}]" }
        environment.restartContainers(containerNames)
    }

    /**
     * Closes all database connections and stops the Docker-Compose environment.
     * @throws ServiceShutdownException if Docker-Compose could not be stopped.
     */
    fun stop() {
        closeDatabaseConnections()
        stopDockerCompose()
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
     * Registers health checks for all containers for which a healthcheck is defined in the Docker-Compose at testcontainers.
     */
    private fun registerHealthChecks() {
        for (container in containers.values) {
            if (container.hasHealthcheck) {
                environment.waitingFor(
                    serviceName = container.name,
                    waitStrategy = Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(dockerConfig.healthTimeout))
                )
            }
        }
    }

    /**
     * Starts Docker-Compose and waits until all containers are healthy.
     * @throws ServiceStartupException if Docker-Compose could not be started.
     * @throws ServiceNotHealthyException if at least one container did not become healthy within the specified timeout.
     */
    private fun startDockerCompose() {
        try {
            environment.start()
        } catch (e: ContainerLaunchException) {
            throw ServiceStartupException("Unable to start Docker Compose: ${e.message}")
        } catch (e: RuntimeException) {
            if (e.cause is ContainerLaunchException && e.cause?.message == "Timed out waiting for container to become healthy") {
                throw ServiceNotHealthyException("At least one container did not become healthy within the specified timeout.")
            }
            throw ServiceStartupException("Unable to start Docker Compose: ${e.message}")
        }
    }

    /**
     * Stops the Docker-Compose environment.
     * @throws ServiceShutdownException if Docker-Compose could not be stopped.
     */
    private fun stopDockerCompose() {
        try {
            environment.stop()
        } catch (e: Exception) {
            throw ServiceShutdownException("Unable to stop Docker-Compose: ${e.message}")
        }
    }

    /**
     * Initializes containers after the Docker-Compose is started.
     * Sets corresponding [DockerContainer] and [org.testcontainers.containers.ContainerState] instances.
     */
    private fun initializeContainers() {
        val dockerContainers = environment.dockerContainers
        for (container in containers.values) {
            val dockerContainerState = environment.getContainerByServiceName(container.name).get()
            container.initialize(dockerContainers[container.name]!!, dockerContainerState)
        }
    }

    private fun closeDatabaseConnections() {
        val databaseConnections = databases.values.mapNotNull { it.connection }
        for (connection in databaseConnections) {
            if (connection is SQLDatabaseConnection) {
                connection.connection.close()
            } else if (connection is MongoDBConnection) {
                connection.client.close()
            }
        }
    }
}
