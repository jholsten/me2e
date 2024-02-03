@file:JvmSynthetic

package org.jholsten.me2e.container.model

import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.Container
import org.testcontainers.containers.ContainerState

/**
 * Wrapper for the reference to the corresponding [DockerContainer] and [ContainerState].
 */
internal class DockerContainerReference(
    /**
     * Container which contains static and dynamic information about the Docker container.
     */
    val container: Container,
    /**
     * State which enables to execute commands in the Docker container.
     */
    val state: ContainerState,

    var info: InspectContainerResponse,
)
