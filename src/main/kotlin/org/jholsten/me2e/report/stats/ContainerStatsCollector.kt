@file:JvmSynthetic

package org.jholsten.me2e.report.stats

import org.jholsten.me2e.container.stats.ContainerStatsConsumer
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry

/**
 * Container stats collector which collects the resource usage statistics for the
 * service with the given name for one test execution.
 */
internal class ContainerStatsCollector(val service: ServiceSpecification) : ContainerStatsConsumer() {
    private val stats: MutableList<AggregatedStatsEntry> = mutableListOf()

    override fun accept(entry: ContainerStatsEntry) {
        stats.add(
            AggregatedStatsEntry(
                service = service,
                timestamp = entry.timestamp,
                memoryUsage = entry.memoryUsage,
                cpuUsage = entry.cpuUsage,
                networkUsage = entry.networkUsage,
                pids = entry.pids,
            )
        )
    }

    /**
     * Resets the list of collected statistics entries.
     * Returns all entries that were collected so far.
     */
    internal fun reset(): List<AggregatedStatsEntry> {
        val entries = stats.toList()
        stats.clear()
        return entries
    }
}
