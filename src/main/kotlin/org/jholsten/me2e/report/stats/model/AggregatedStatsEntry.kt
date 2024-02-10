package org.jholsten.me2e.report.stats.model

import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import java.time.Instant

/**
 * Model representing one resource usage statistics entry from one Docker container.
 */
class AggregatedStatsEntry internal constructor(
    /**
     * Container for which this entry was collected.
     */
    val service: ServiceSpecification,

    /**
     * Timestamp of when this entry was received.
     */
    timestamp: Instant,

    /**
     * Statistics about the memory usage of the container.
     */
    memoryUsage: MemoryUsage,

    /**
     * Statistics about the CPU usage of the container.
     */
    cpuUsage: CpuUsage,

    /**
     * Statistics about the network usage of the container.
     */
    networkUsage: NetworkUsage,

    /**
     * Number of processes or threads the container has created.
     */
    pids: Long?,
) : ContainerStatsEntry(
    timestamp = timestamp,
    memoryUsage = memoryUsage,
    cpuUsage = cpuUsage,
    networkUsage = networkUsage,
    pids = pids,
)
