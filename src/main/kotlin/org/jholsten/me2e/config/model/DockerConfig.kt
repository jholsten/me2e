package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.container.docker.DockerComposeRemoveImagesStrategy
import org.jholsten.me2e.container.docker.DockerComposeVersion

/**
 * Configuration for Docker/Docker-Compose.
 */
class DockerConfig(
    /**
     * Docker-Compose version to use.
     */
    @JsonProperty("docker-compose-version")
    val dockerComposeVersion: DockerComposeVersion = DockerComposeVersion.V2,

    /**
     * Whether to always build images before starting containers.
     *
     * Applies `--build` in `docker compose up` (see [Docker Documentation](https://docs.docker.com/engine/reference/commandline/compose_up/#options).
     */
    @JsonProperty("build-images")
    val buildImages: Boolean = false,

    /**
     * Whether to remove images used by services after containers shut down.
     *
     * If not set to [DockerComposeRemoveImagesStrategy.NONE], applies `--rmi=local` or `--rmi=all` in `docker compose down`
     * (see [Docker Documentation](https://docs.docker.com/engine/reference/commandline/compose_down/#options).
     */
    @JsonProperty("remove-images")
    val removeImages: DockerComposeRemoveImagesStrategy = DockerComposeRemoveImagesStrategy.NONE,

    /**
     * Whether to remove volumes after containers shut down.
     *
     * Applies `--volumes` in `docker compose down` (see [Docker Documentation](https://docs.docker.com/engine/reference/commandline/compose_down/#options).
     */
    @JsonProperty("remove-volumes")
    val removeVolumes: Boolean = true,

    /**
     * Number of seconds to wait at most until containers are healthy.
     * Only applicable if at least one healthcheck is defined in Docker-Compose.
     */
    @JsonProperty("health-timeout")
    val healthTimeout: Long = 10,
) {
}
