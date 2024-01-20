package org.jholsten.me2e.report.logs.model

/**
 * List of log entries that were collected from all containers.
 */
class AggregatedLogEntryList(entries: Collection<AggregatedLogEntry> = listOf()) : ArrayList<AggregatedLogEntry>(entries.toList()) {
    override fun toString(): String {
        return this.joinToString("")
    }
}
