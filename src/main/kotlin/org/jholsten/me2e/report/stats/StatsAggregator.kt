package org.jholsten.me2e.report.stats

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry
import org.jholsten.me2e.utils.logger

/**
 * Service which collects all statistics entries for all containers.
 */
internal class StatsAggregator internal constructor() {
    private val logger = logger<StatsAggregator>()

    /**
     * Map of container specification and statistics entry collector.
     * For each container, there is one collector which captures the container's resource usage statistics.
     */
    private val consumers: MutableMap<ServiceSpecification, ContainerStatsCollector> = mutableMapOf()

    /**
     * Initializes the collector for consuming container statistics when the [container] was started.
     * Attaches statistics consumer to the container to start capturing its resource usage statistics.
     * @param container Container which has been started and whose resource usage should be captured.
     * @param specification Representation of the [container] instance.
     */
    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        val consumer = ContainerStatsCollector(specification)
        this.consumers[specification] = consumer
        container.addStatsConsumer(consumer)
        logger.info("Initialized container stats aggregator for container ${container.name}.")
    }

    /**
     * Callback function to execute when the execution of all tests has finished. Collects statistics from all containers.
     * @return Collected statistics entries.
     */
    @JvmSynthetic
    internal fun collectStats(): List<AggregatedStatsEntry> {
        val stats = mutableListOf<AggregatedStatsEntry>()
        for (consumer in consumers.values) {
            stats.addAll(consumer.collect())
        }
        stats.sortBy { it.timestamp }
        return stats.toList()
    }
}
