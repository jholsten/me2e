package org.jholsten.me2e.container.model

import org.jholsten.me2e.container.docker.DockerCompose
import com.github.dockerjava.api.model.Container as DockerContainer
import org.testcontainers.containers.ContainerState

/**
 * Wrapper for the reference to the corresponding [DockerContainer] and [ContainerState]
 * of a container. Also contains a reference to the Docker compose environment which the container is part of.
 */
internal class DockerContainerReference(
    /**
     * Container which contains static and dynamic information about the Docker container.
     * The information may be outdated and is only updated when the container is started or restarted.
     */
    @JvmSynthetic
    var container: DockerContainer,

    /**
     * State which enables to execute commands in the Docker container.
     */
    @JvmSynthetic
    val state: ContainerState,

    /**
     * Docker compose environment the container is part of.
     */
    @JvmSynthetic
    val environment: DockerCompose,
)
