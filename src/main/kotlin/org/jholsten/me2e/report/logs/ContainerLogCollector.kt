package org.jholsten.me2e.report.logs

import org.jholsten.me2e.container.logging.ContainerLogConsumer
import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification

/**
 * Log collector which collects the logs of the container with the given name.
 */
internal class ContainerLogCollector(val service: ServiceSpecification) : ContainerLogConsumer() {
    /**
     * Logs which were collected so far from the corresponding container.
     */
    private val logs: MutableList<AggregatedLogEntry> = mutableListOf()

    @JvmSynthetic
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
     * Returns all log entries that were collected so far and resets the list of collected entries.
     * @return Log entries collected so far from the corresponding container.
     */
    @JvmSynthetic
    fun collect(): List<AggregatedLogEntry> {
        val entries = logs.toList()
        logs.clear()
        return entries
    }
}
