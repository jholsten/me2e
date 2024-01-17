package org.jholsten.me2e.report.logs

import org.jholsten.me2e.container.Container

/**
 * Service which collects all log entries from all containers.
 * After each test execution, the logs for this test are collected
 * from all containers and stored in [logs].
 */
class LogAggregator(
    /**
     * Containers of the environment.
     */
    private val containers: Collection<Container>,
) {
    /**
     * Map of container name and log collector.
     * For each container, there is one collector which collects
     * the container's logs of the current test execution.
     */
    private val consumers: Map<String, TestLogCollector> = containers.associate { it.name to TestLogCollector(it.name) }

    /**
     * Logs for each unique test ID that were collected so far.
     */
    private val logs: MutableMap<String, List<LogEntry>> = mutableMapOf()

    /**
     * Initializes the collector by attaching log consumers to all containers.
     */
    init {
        for (container in containers) {
            container.addLogConsumer(consumers[container.name]!!)
        }
    }

    /**
     * Callback function to execute when one test execution finished.
     * Collects logs from all containers and stores them with the
     * reference to the corresponding test.
     */
    @JvmSynthetic
    internal fun collectLogs(testId: String) {
        val logs = mutableListOf<LogEntry>()
        for (container in containers) {
            logs.addAll(consumers[container.name]!!.reset())
        }
        logs.sortBy { it.timestamp }
        this.logs[testId] = logs
    }

    /**
     * Returns aggregated logs which where collected for the execution of the test with the given ID.
     * @throws IllegalArgumentException if no logs are stored for the given test ID.
     */
    fun getAggregatedLogsByTestId(testId: String): AggregatedLogEntryList {
        return requireNotNull(logs[testId]?.let { AggregatedLogEntryList(it) }) { "No logs stored for test with ID $testId." }
    }

    /**
     * Returns aggregated logs of all test executions as map of test ID and aggregated logs.
     */
    fun getAggregatedLogs(): Map<String, AggregatedLogEntryList> {
        return logs.map { (testId, logs) -> testId to AggregatedLogEntryList(logs) }.toMap()
    }
}
