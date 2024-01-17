package org.jholsten.me2e.container.logging.model

/**
 * List of log entries that were collected from a container.
 */
class ContainerLogEntryList(entries: Collection<ContainerLogEntry> = listOf()) : ArrayList<ContainerLogEntry>(entries.toList()) {
    override fun toString(): String {
        return this.joinToString("")
    }
}
