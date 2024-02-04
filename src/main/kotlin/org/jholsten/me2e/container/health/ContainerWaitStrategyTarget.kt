package org.jholsten.me2e.container.health

import com.github.dockerjava.api.command.InspectContainerResponse
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget

/**
 * Wait strategy for a container as part of a Docker compose.
 */
internal class ContainerWaitStrategyTarget(private val containerId: String) : WaitStrategyTarget {
    @JvmSynthetic
    override fun getExposedPorts(): MutableList<Int> = mutableListOf()

    @JvmSynthetic
    override fun getContainerInfo(): InspectContainerResponse {
        return dockerClient.inspectContainerCmd(containerId).exec()
    }

    @JvmSynthetic
    override fun getContainerId(): String = containerId
}
