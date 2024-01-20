package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.LoggerContext
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.logs.model.LogEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service which collects all log entries from all containers and from the Test Runner.
 * After each test execution, the logs for this test are collected from all containers
 * and from the Test Runner and stored in [logs].
 */
class LogAggregator(
) {
    companion object {
        /**
         * Name of the service which represents the Test Runner.
         */
        @JvmStatic
        val TEST_RUNNER_NAME = "Test Runner"
    }

    /**
     * Map of container name and log collector.
     * For each container, there is one collector which collects
     * the container's logs of the current test execution.
     */
    private var consumers: Map<String, ContainerLogCollector> = mapOf()

    private val testRunnerLogCollector: TestRunnerLogCollector = TestRunnerLogCollector()

    /**
     * Logs for each unique test ID that were collected so far.
     */
    private val logs: MutableMap<String, List<LogEntry>> = mutableMapOf()

    @JvmSynthetic
    internal fun initializeBeforeContainersStarted() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        testRunnerLogCollector.context = loggerContext
        testRunnerLogCollector.name = "TestRunnerLogCollector"
        testRunnerLogCollector.start()
        val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        root.addAppender(testRunnerLogCollector)
    }

    /**
     * Initializes the collector by attaching log consumers to all containers.
     */
    @JvmSynthetic
    internal fun initializeOnContainersStarted(containers: Collection<Container>) {
        this.consumers = containers.associate { it.name to ContainerLogCollector(it.name) }
        for (container in containers) {
            container.addLogConsumer(consumers[container.name]!!)
        }
    }

    /**
     * Callback function to execute when one test execution finished.
     * Collects logs from all containers and stores them with the
     * reference to the corresponding test.
     * @return Collected log entries.
     */
    @JvmSynthetic
    internal fun collectLogs(testId: String): AggregatedLogEntryList {
        val logs = mutableListOf<LogEntry>()
        for (consumer in consumers.values) {
            logs.addAll(consumer.reset())
        }
        logs.addAll(testRunnerLogCollector.reset())
        logs.sortBy { it.timestamp }
        this.logs[testId] = logs
        return AggregatedLogEntryList(logs)
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
