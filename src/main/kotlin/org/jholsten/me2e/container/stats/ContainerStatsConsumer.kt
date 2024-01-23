package org.jholsten.me2e.container.stats

import com.github.dockerjava.api.model.CpuStatsConfig
import com.github.dockerjava.api.model.MemoryStatsConfig
import com.github.dockerjava.api.model.Statistics
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

/**
 * Base class for consuming statistics of a container.
 * Whenever docker sends a new statistics entry for the container, the callback function [accept] is executed.
 */
abstract class ContainerStatsConsumer : Consumer<Statistics> {
    private val logger = LoggerFactory.getLogger(ContainerStatsConsumer::class.java)
    private val mc: MathContext = MathContext(5)

    /**
     * Callback function to execute when a new statistics entry is received for a container.
     * @param entry Aggregated statistics entry received from the Docker container.
     */
    abstract fun accept(entry: ContainerStatsEntry)

    override fun accept(t: Statistics) {
        try {
            accept(aggregateStatistics(t))
        } catch (e: Exception) {
            logger.error("Exception occurred while trying to consume statistics entry:", e)
        }
    }

    /**
     * Aggregates the resource usage information from the given [statistics] entry to an instance of [ContainerStatsEntry].
     * To aggregate the metrics, the formulas given in the [Docker Documentation](https://docs.docker.com/engine/api/v1.44/#tag/Container/operation/ContainerStats)
     * are used.
     * @param statistics Statistics entry to aggregate.
     */
    private fun aggregateStatistics(statistics: Statistics): ContainerStatsEntry {
        val memoryUsage = aggregateMemoryUsage(statistics.memoryStats)
        val cpuUsage = aggregateCpuUsage(statistics)
        val networkUsage = aggregateNetworkUsage(statistics)
        return ContainerStatsEntry(
            timestamp = Instant.parse(statistics.read).truncatedTo(ChronoUnit.SECONDS),
            memoryUsage = memoryUsage,
            cpuUsage = cpuUsage,
            networkUsage = networkUsage,
            pids = statistics.pidsStats.current,
        )
    }

    /**
     * Aggregates the memory usage of the container using the given [memoryStats] entry.
     */
    private fun aggregateMemoryUsage(memoryStats: MemoryStatsConfig): ContainerStatsEntry.MemoryUsage {
        val usedMemory = memoryStats.usedMemory
        val availableMemory = memoryStats.limit
        return ContainerStatsEntry.MemoryUsage(
            usedMemory = usedMemory,
            availableMemory = availableMemory,
            percentage = calculateMemoryPercentage(usedMemory, availableMemory),
        )
    }

    /**
     * Aggregates the CPU usage of the container using the given [statistics] entry.
     */
    private fun aggregateCpuUsage(statistics: Statistics): ContainerStatsEntry.CpuUsage {
        val cpuDelta = statistics.cpuDelta
        val systemCpuDelta = statistics.systemCpuDelta
        val numberOfCpus = statistics.cpuStats.numberOfCpus
        return ContainerStatsEntry.CpuUsage(
            percentage = calculateCpuPercentage(cpuDelta, systemCpuDelta, numberOfCpus),
        )
    }

    /**
     * Aggregates the network usage of the container using the given [statistics] entry.
     */
    private fun aggregateNetworkUsage(statistics: Statistics): ContainerStatsEntry.NetworkUsage {
        val networks = statistics.networks
        val received = networks?.values?.mapNotNull { it.rxBytes }?.sum()
        val sent = networks?.values?.mapNotNull { it.txBytes }?.sum()
        return ContainerStatsEntry.NetworkUsage(
            received = received,
            sent = sent,
        )
    }

    /**
     * Calculates the memory usage percentage based on the given [usedMemory] and [availableMemory].
     * Returns `null` if any of the given parameters is `null`.
     */
    private fun calculateMemoryPercentage(usedMemory: Long?, availableMemory: Long?): BigDecimal? {
        return if (usedMemory != null && availableMemory != null) {
            try {
                (BigDecimal(usedMemory).divide(BigDecimal(availableMemory), mc)) * BigDecimal(100)
            } catch (e: ArithmeticException) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Calculates the CPU usage percentage based on the given [cpuDelta], [systemCpuDelta] and [numberOfCpus].
     * Returns `null` if any of the given parameters is `null`.
     */
    private fun calculateCpuPercentage(cpuDelta: Long?, systemCpuDelta: Long?, numberOfCpus: Long?): BigDecimal? {
        return if (cpuDelta != null && systemCpuDelta != null && numberOfCpus != null) {
            try {
                (BigDecimal(cpuDelta).divide(BigDecimal(systemCpuDelta), mc)) * BigDecimal(numberOfCpus) * BigDecimal(100)
            } catch (e: ArithmeticException) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Extension property to calculate the used memory as given in [MemoryStatsConfig].
     */
    private val MemoryStatsConfig.usedMemory: Long?
        get() {
            val usage = this.usage
            val cache = this.stats?.cache
            return when {
                usage != null && cache != null -> usage - cache
                else -> null
            }
        }

    /**
     * Extension property to calculate the CPU delta as given in [Statistics].
     */
    private val Statistics.cpuDelta: Long?
        get() {
            val totalCpuUsage = this.cpuStats.cpuUsage?.totalUsage
            val totalPreCpuUsage = this.preCpuStats.cpuUsage?.totalUsage
            return when {
                totalCpuUsage != null && totalPreCpuUsage != null -> totalCpuUsage - totalPreCpuUsage
                else -> null
            }
        }

    /**
     * Extension property to calculate the System CPU delta as given in [Statistics].
     */
    private val Statistics.systemCpuDelta: Long?
        get() {
            val cpuUsage = this.cpuStats.systemCpuUsage
            val preCpuUsage = this.preCpuStats.systemCpuUsage
            return when {
                cpuUsage != null && preCpuUsage != null -> cpuUsage - preCpuUsage
                else -> null
            }
        }

    /**
     * Extension property to calculate the number of CPUs as given in [CpuStatsConfig].
     */
    private val CpuStatsConfig.numberOfCpus: Long?
        get() {
            val perCpuUsage = this.cpuUsage?.percpuUsage
            return when {
                perCpuUsage != null -> perCpuUsage.size.toLong()
                else -> this.onlineCpus
            }
        }
}
