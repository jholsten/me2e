package org.jholsten.me2e.container.stats

import org.jholsten.me2e.container.stats.model.ContainerStatsEntry

/**
 * Container stats consumer which collects all statistics entries and
 * stores them in a local variable [stats].
 */
class ContainerStatsCollector : ContainerStatsConsumer() {
    /**
     * Statistics entries of the container which were collected so far.
     */
    val stats: MutableList<ContainerStatsEntry> = mutableListOf()

    override fun accept(entry: ContainerStatsEntry) {
        stats.add(entry)
    }
}
