package org.jholsten.me2e.container.logging

import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import org.jholsten.me2e.container.logging.model.ContainerLogEntryList

/**
 * Log consumer which collects all logs and stores them in
 * a local variable [logs].
 */
open class ContainerLogCollector : ContainerLogConsumer() {
    /**
     * Logs of the container which were collected so far.
     */
    val logs: ContainerLogEntryList = ContainerLogEntryList()

    override fun accept(entry: ContainerLogEntry) {
        logs.add(entry)
    }
}
