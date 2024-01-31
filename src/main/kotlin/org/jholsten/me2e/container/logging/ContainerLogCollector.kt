package org.jholsten.me2e.container.logging

import org.jholsten.me2e.container.logging.model.ContainerLogEntry

/**
 * Log consumer which collects all logs and stores them in
 * a local variable [logs].
 */
open class ContainerLogCollector : ContainerLogConsumer() {
    /**
     * Logs of the container which were collected so far.
     */
    val logs: MutableList<ContainerLogEntry> = mutableListOf()

    override fun accept(entry: ContainerLogEntry) {
        logs.add(entry)
    }
}
