package org.jholsten.me2e.report.stats

import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntry
import org.jholsten.me2e.report.stats.model.AggregatedStatsEntryList
import org.jholsten.me2e.utils.logger

class StatsAggregator internal constructor() {
    private val logger = logger(this)

    /**
     * Map of container name and statistics entry collector.
     * For each container, there is one collector which collects the container's
     * resource usage statistics for the current test execution.
     */
    private var consumers: Map<String, ContainerStatsCollector> = mapOf()

    /**
     * Container statistics for each unique test ID that were collected so far.
     */
    private val stats: MutableMap<String, List<AggregatedStatsEntry>> = mutableMapOf()

    /**
     * Initializes the collector for consuming container statistics when the [containers] were started.
     * Attaches statistics consumers to all containers to start capturing their resource usage statistics.
     */
    @JvmSynthetic
    internal fun initializeOnContainersStarted(containers: Collection<Container>) {
        this.consumers = containers.associate { it.name to ContainerStatsCollector(it.name) }
        for (container in containers) {
            container.addStatsConsumer(consumers[container.name]!!)
        }
        logger.info("Initialized container stats aggregator.")
    }

    /**
     * Callback function to execute when one test execution finished.
     * Collects statistics from all containers and stores them with the
     * reference to the corresponding test.
     * @return Collected statistics entries.
     */
    @JvmSynthetic
    internal fun collectStats(testId: String): AggregatedStatsEntryList {
        val stats = mutableListOf<AggregatedStatsEntry>()
        for (consumer in consumers.values) {
            stats.addAll(consumer.reset())
        }
        stats.sortBy { it.timestamp }
        this.stats[testId] = stats
        return AggregatedStatsEntryList(stats.toList())
    }

    /**
     * Returns aggregated statistics which were collected for the execution of the test with the given ID.
     * @throws IllegalArgumentException if no statistics are stored for the given test ID.
     */
    fun getAggregatedStatsByTestId(testId: String): AggregatedStatsEntryList {
        return requireNotNull(stats[testId]?.let { AggregatedStatsEntryList(it) }) { "No stats stored for test with ID $testId" }
    }

    /**
     * Returns aggregated statistics of all test executions as map of test ID and aggregated statistics entries.
     */
    fun getAggregatedStats(): Map<String, AggregatedStatsEntryList> {
        return stats.map { (testId, stats) -> testId to AggregatedStatsEntryList(stats) }.toMap()
    }
}
