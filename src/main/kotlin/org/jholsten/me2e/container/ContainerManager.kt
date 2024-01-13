package org.jholsten.me2e.container

import com.github.dockerjava.api.model.Container as DockerContainer
import org.apache.commons.lang3.RandomStringUtils
import org.jholsten.me2e.config.model.DockerConfig
import org.jholsten.me2e.container.database.DatabaseContainer
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
     * Identifier of the services to start. Will be used as a prefix for all container names.
     */
    private val identifier: String = "me2e_${RandomStringUtils.randomAlphabetic(3)}".lowercase()

    /**
     * Reference to the Docker-Compose Container.
     */
    private val environment = DockerCompose(identifier, dockerComposeFile, version = dockerConfig.dockerComposeVersion)
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
     * Stops the Docker-Compose environment. It is not necessary to stop the environment manually, as it is automatically shut
     * down as soon as the tests finish running.
     * @throws ServiceShutdownException if Docker-Compose could not be stopped.
     */
    fun stop() {
        try {
            environment.stop()
        } catch (e: Exception) {
            throw ServiceShutdownException("Unable to stop Docker-Compose: ${e.message}")
        }
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
     * Initializes containers after the Docker-Compose is started.
     * Sets corresponding [DockerContainer] and [org.testcontainers.containers.ContainerState] instances.
     */
    private fun initializeContainers() {
        val dockerContainers = getDockerContainers()
        for (container in containers.values) {
            val dockerContainerState = environment.getContainerByServiceName(container.name).get()
            container.initialize(dockerConfig, dockerContainers[container.name]!!, dockerContainerState)
        }
    }

    /**
     * Returns map of service name and [DockerContainer] instance for all services in the Docker-Compose file.
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
