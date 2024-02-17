package org.jholsten.me2e.config.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jholsten.me2e.container.docker.DockerComposeVersion

/**
 * Configuration for Docker/Docker-Compose.
 */
data class DockerConfig internal constructor(
    /**
     * Docker-Compose version to use.
     */
    @JsonProperty("docker-compose-version")
    val dockerComposeVersion: DockerComposeVersion = DockerComposeVersion.V2,

    /**
     * Policy on pulling Docker images.
     * With this setting, the pull policy is set globally, but can be overwritten for individual containers by setting
     * the label `org.jholsten.me2e.pull-policy`.
     * @see org.jholsten.me2e.container.Container.pullPolicy
     */
    @JsonProperty("pull-policy")
    val pullPolicy: PullPolicy = PullPolicy.MISSING,

    /**
     * Whether to always build images before starting containers.
     *
     * Applies `--build` in `docker compose up`.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/compose_up/#options">Docker Documentation</a>
     */
    @JsonProperty("build-images")
    val buildImages: Boolean = false,

    /**
     * Whether to remove images used by services after containers shut down.
     *
     * If not set to [DockerComposeRemoveImagesStrategy.NONE], applies `--rmi=local` or `--rmi=all` in `docker compose down`.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/compose_down/#options">Docker Documentation</a>
     */
    @JsonProperty("remove-images")
    val removeImages: DockerComposeRemoveImagesStrategy = DockerComposeRemoveImagesStrategy.NONE,

    /**
     * Whether to remove volumes after containers shut down.
     *
     * Applies `--volumes` in `docker compose down`.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/compose_down/#options">Docker Documentation</a>
     */
    @JsonProperty("remove-volumes")
    val removeVolumes: Boolean = true,

    /**
     * Number of seconds to wait at most until containers are healthy.
     * Only applicable if at least one healthcheck is defined in Docker-Compose.
     */
    @JsonProperty("health-timeout")
    val healthTimeout: Long = 30,
) {

    /**
     * Available policies specifying whether to pull existing Docker images.
     */
    enum class PullPolicy {
        /**
         * Instructs Docker to only pull missing Docker images.
         */
        MISSING,

        /**
         * Instructs Docker to always pull the latest version of all Docker images.
         */
        ALWAYS,
    }

    /**
     * Available strategies that can be used to specify whether and which images used by
     * services should be removed after the containers shut down.
     *
     * If not [NONE], applies `--rmi=${strategy.lowercase()}` in `docker compose down` command.
     * @see <a href="https://docs.docker.com/engine/reference/commandline/compose_down/#options">Docker Documentation</a>
     */
    enum class DockerComposeRemoveImagesStrategy {
        /**
         * Do not remove any images after the containers shut down.
         */
        NONE,

        /**
         * Remove all images used by services in Docker-Compose.
         */
        ALL,

        /**
         * Remove only images used by services in Docker-Compose that don't have a custom tag.
         */
        LOCAL
    }
}