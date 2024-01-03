package org.jholsten.me2e.container.docker

/**
 * Available strategies that can be used to specify whether and which images used by
 * services should be removed after the containers shut down.
 *
 * If not [NONE], applies `--rmi=${strategy.lowercase()}` in `docker compose down` command.
 * (See [Docker Documentation](https://docs.docker.com/engine/reference/commandline/compose_down/#options)
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
