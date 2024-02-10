package org.jholsten.me2e.container.stats.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Model representing one statistics entry of a container returned by `docker stats`.
 * Contains aggregated resource usage information about CPU, memory and network input and output.
 * @see com.github.dockerjava.api.model.Statistics
 */
open class ContainerStatsEntry internal constructor(
    /**
     * Timestamp of when this entry was received.
     */
    val timestamp: Instant,

    /**
     * Statistics about the memory usage of the container.
     */
    val memoryUsage: MemoryUsage,

    /**
     * Statistics about the CPU usage of the container.
     */
    val cpuUsage: CpuUsage,

    /**
     * Statistics about the network usage of the container.
     */
    val networkUsage: NetworkUsage,

    /**
     * Number of processes or threads the container has created.
     */
    val pids: Long?,
) {
    /**
     * Statistics about the memory usage of the container.
     */
    data class MemoryUsage(
        /**
         * Total amount of memory the container is using, given in number of bytes.
         */
        val usedMemory: Long?,

        /**
         * Total amount of memory the container is allowed to use, given in number of bytes.
         */
        val availableMemory: Long?,

        /**
         * Percentage of the host's memory the container is using.
         */
        val percentage: BigDecimal?,
    )

    /**
     * Statistics about the CPU usage of the container.
     */
    data class CpuUsage(
        /**
         * Percentage of the host's CPU the container is using.
         */
        val percentage: BigDecimal?,
    )

    /**
     * Statistics about the network usage of the container.
     */
    data class NetworkUsage(
        /**
         * Number of bytes the container has received over its network interface.
         */
        val received: Long?,

        /**
         * Number of bytes the container has sent over its network interface.
         */
        val sent: Long?,
    )
}
