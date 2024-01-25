@file:JvmSynthetic

package org.jholsten.me2e.report.logs

import org.jholsten.me2e.container.logging.ContainerLogConsumer
import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification

/**
 * Log collector which collects the logs of the service with the given name
 * for one test execution.
 */
internal class ContainerLogCollector(val service: ServiceSpecification) : ContainerLogConsumer() {
    private val logs: MutableList<AggregatedLogEntry> = mutableListOf()

    override fun accept(entry: ContainerLogEntry) {
        logs.add(
            AggregatedLogEntry(
                service = service,
                timestamp = entry.timestamp,
                message = entry.message
            )
        )
    }

    /**
     * Resets the list of collected log entries.
     * Returns all entries that were collected so far.
     */
    internal fun collect(): List<AggregatedLogEntry> {
        val entries = logs.toList()
        logs.clear()
        return entries
    }
}
