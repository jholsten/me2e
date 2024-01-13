package org.jholsten.me2e.container.docker

import org.jholsten.me2e.container.exception.DockerException
import org.jholsten.me2e.utils.logger
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.ContainerState
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
import java.util.Optional

/**
 * Custom wrapper for testcontainers Docker-Compose classes [DockerComposeV1] and [DockerComposeV2].
 * Since testcontainers uses two different classes for version 1 and 2, which do not have a common supertype, it is otherwise difficult
 * to handle instances of both classes. Therefore, this class provides a common interface for both classes.
 */
class DockerCompose(
    identifier: String,
    private val file: File,
    private val version: DockerComposeVersion
) {
    private val logger = logger(this)

    /**
     * Kotlin wrapper for [DockerComposeV1] to avoid type issue.
     * See [GitHub Issue #1010](https://github.com/testcontainers/testcontainers-java/issues/1010)
     */
    internal class KDockerComposeV1(identifier: String, file: File) : DockerComposeV1<KDockerComposeV1>(identifier, file)

    /**
     * Lazy reference to testcontainers implementation for Docker-Compose version 1.
     */
    private val v1: KDockerComposeV1 by lazy {
        KDockerComposeV1(identifier, file)
    }

    /**
     * Lazy reference to testcontainers implementation for Docker-Compose version 2.
     */
    private val v2: DockerComposeV2 by lazy {
        DockerComposeV2(identifier, file)
    }

    /**
     * Component to execute custom Docker-Compose commands locally.
     */
    private val local: Local by lazy { Local() }

    /**
     * Whether to use a local Docker-Compose binary instead of a container.
     * @see DockerComposeV1.withLocalCompose
     * @see DockerComposeV2.withLocalCompose
     */
    fun withLocalCompose(localCompose: Boolean) = apply {
        when (this.version) {
            DockerComposeVersion.V1 -> v1.withLocalCompose(localCompose)
            DockerComposeVersion.V2 -> v2.withLocalCompose(localCompose)
        }
    }

    /**
     * Whether to always build images before starting containers.
     * @see DockerComposeV1.withBuild
     * @see DockerComposeV2.withBuild
     */
    fun withBuild(build: Boolean) = apply {
        when (this.version) {
            DockerComposeVersion.V1 -> v1.withBuild(build)
            DockerComposeVersion.V2 -> v2.withBuild(build)
        }
    }

    /**
     * Whether to remove images after containers shut down.
     * @see DockerComposeV1.withRemoveImages
     * @see DockerComposeV2.withRemoveImages
     */
    fun withRemoveImages(removeImages: DockerComposeRemoveImagesStrategy) = apply {
        when (this.version) {
            DockerComposeVersion.V1 -> v1.withRemoveImages(removeImages.toV1())
            DockerComposeVersion.V2 -> v2.withRemoveImages(removeImages.toV2())
        }
    }

    /**
     * Whether to remove volumes after containers shut down.
     * @see DockerComposeV1.withRemoveVolumes
     * @see DockerComposeV2.withRemoveVolumes
     */
    fun withRemoveVolumes(removeVolumes: Boolean) = apply {
        when (this.version) {
            DockerComposeVersion.V1 -> v1.withRemoveVolumes(removeVolumes)
            DockerComposeVersion.V2 -> v2.withRemoveVolumes(removeVolumes)
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
            DockerComposeVersion.V1 -> v1.waitingFor(serviceName, waitStrategy)
            DockerComposeVersion.V2 -> v2.waitingFor(serviceName, waitStrategy)
        }
    }

    /**
     * Returns [ContainerState] for the service with the given name.
     * @see DockerComposeV1.getContainerByServiceName
     * @see DockerComposeV2.getContainerByServiceName
     */
    fun getContainerByServiceName(serviceName: String): Optional<ContainerState> {
        return when (this.version) {
            DockerComposeVersion.V1 -> v1.getContainerByServiceName(serviceName)
            DockerComposeVersion.V2 -> v2.getContainerByServiceName(serviceName)
        }
    }

    /**
     * Starts the Docker-Compose container.
     * @see DockerComposeV1.start
     * @see DockerComposeV2.start
     */
    fun start() {
        when (this.version) {
            DockerComposeVersion.V1 -> v1.start()
            DockerComposeVersion.V2 -> v2.start()
        }
    }

    /**
     * Stops the Docker-Compose container.
     * @see DockerComposeV1.stop
     * @see DockerComposeV2.stop
     */
    fun stop() {
        when (this.version) {
            DockerComposeVersion.V1 -> v1.stop()
            DockerComposeVersion.V2 -> v2.stop()
        }
    }

    /**
     * Pulls images for services with the given names.
     */
    fun pull(servicesToPull: List<String>) {
        if (servicesToPull.isNotEmpty()) {
            logger.info("Pulling images for services: [${servicesToPull.joinToString(", ")}]...")
            execute("pull ${servicesToPull.joinToString(" ")}")
        }
    }

    /**
     * Executes the given Docker-Compose command locally.
     * Redirects output and error to logger.
     * @throws DockerException in case of errors.
     */
    fun execute(command: String) {
        local.execute(command)
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
            "COMPOSE_FILE" to file.absolutePath
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
            val baseCommand = when (version) {
                DockerComposeVersion.V1 -> ComposeContainer.COMPOSE_EXECUTABLE
                DockerComposeVersion.V2 -> ComposeContainer.COMPOSE_EXECUTABLE + " compose"
            }

            val cmd = Splitter
                .onPattern(" ")
                .omitEmptyStrings()
                .splitToList("$baseCommand $command")

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
                throw DockerException("Running Docker-Compose command $cmd failed: ${e.message}")
            }
        }
    }

    private fun DockerComposeRemoveImagesStrategy.toV1(): DockerComposeV1.RemoveImages? {
        return when (this) {
            DockerComposeRemoveImagesStrategy.NONE -> null
            DockerComposeRemoveImagesStrategy.ALL -> DockerComposeV1.RemoveImages.ALL
            DockerComposeRemoveImagesStrategy.LOCAL -> DockerComposeV1.RemoveImages.LOCAL
        }
    }

    private fun DockerComposeRemoveImagesStrategy.toV2(): DockerComposeV2.RemoveImages? {
        return when (this) {
            DockerComposeRemoveImagesStrategy.NONE -> null
            DockerComposeRemoveImagesStrategy.ALL -> DockerComposeV2.RemoveImages.ALL
            DockerComposeRemoveImagesStrategy.LOCAL -> DockerComposeV2.RemoveImages.LOCAL
        }
    }
}
