package org.jholsten.me2e.container.docker

/**
 * Supported Docker-Compose versions.
 */
enum class DockerComposeVersion {
    /**
     * Execute commands using `docker-compose`.
     */
    @Deprecated(
        message = "Docker-Compose version 1 is deprecated since June 2023. See https://docs.docker.com/compose/compose-file/compose-versioning/.",
        replaceWith = ReplaceWith("DockerComposeVersion.V2"),
    )
    V1,

    /**
     * Execute commands using the `docker compose` CLI plugin.
     */
    V2,
}
