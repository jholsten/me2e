package org.jholsten.me2e.report.logs.model

/**
 * List of log entries that were collected from all containers.
 */
class AggregatedLogEntryList(entries: Collection<LogEntry> = listOf()) : ArrayList<LogEntry>(entries.toList()) {
    override fun toString(): String {
        return this.joinToString("")
    }
}
