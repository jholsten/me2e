package org.jholsten.me2e.container.health

import com.github.dockerjava.api.command.InspectContainerResponse
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget

/**
 * Wait strategy for a container as part of a Docker compose.
 */
internal class ContainerWaitStrategyTarget(private val containerId: String) : WaitStrategyTarget {
    override fun getExposedPorts(): MutableList<Int> {
        return mutableListOf()
    }

    override fun getContainerInfo(): InspectContainerResponse {
        return dockerClient.inspectContainerCmd(containerId).exec()
    }

    override fun getContainerId(): String {
        return containerId
    }
}
