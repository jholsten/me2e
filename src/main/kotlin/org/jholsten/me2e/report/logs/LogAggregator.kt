package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.model.AggregatedLogEntryList
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service which collects all log entries from all containers and from the Test Runner.
 * After each test execution, the logs for this test are collected from all containers
 * and from the Test Runner and stored in [logs].
 */
class LogAggregator internal constructor() {
    companion object {
        /**
         * Name of the service which represents the Test Runner.
         */
        @JvmStatic
        val TEST_RUNNER_NAME = "Test Runner"
    }

    /**
     * Map of container specification and log collector.
     * For each container instance, there is one collector which collects
     * the container's logs of the current test execution.
     */
    private val consumers: MutableMap<ServiceSpecification, ContainerLogCollector> = mutableMapOf()

    /**
     * Consumer of the Test Runner's logs.
     * Records all logs which are output to any SLF4J logger.
     */
    private lateinit var testRunnerLogCollector: TestRunnerLogCollector

    /**
     * Logs for each unique test ID that were collected so far.
     */
    private val logs: MutableMap<String, List<AggregatedLogEntry>> = mutableMapOf()

    /**
     * Initializes the collector for consuming the Test Runner's logs when the test execution started.
     * Attaches consumer to the root logger to start capturing its logs.
     */
    @JvmSynthetic
    internal fun initializeOnTestExecutionStarted() {
        consumeTestRunnerLogs()
    }

    /**
     * Initializes the collector for consuming container logs when the [container] was started.
     * Attaches log consumer to the container to start capturing its logs.
     */
    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        val consumer = ContainerLogCollector(specification)
        this.consumers[specification] = consumer
        container.addLogConsumer(consumer)
    }

    /**
     * Callback function to execute when one test execution finished.
     * Collects logs from all containers and stores them with the
     * reference to the corresponding test.
     * @return Collected log entries.
     */
    @JvmSynthetic
    internal fun collectLogs(testId: String): AggregatedLogEntryList {
        val logs = mutableListOf<AggregatedLogEntry>()
        for (consumer in consumers.values) {
            logs.addAll(consumer.collect())
        }
        logs.addAll(testRunnerLogCollector.reset())
        logs.sortBy { it.timestamp }
        this.logs[testId] = logs
        return AggregatedLogEntryList(logs)
    }

    /**
     * Returns aggregated logs which were collected for the execution of the test with the given ID.
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

    private fun consumeTestRunnerLogs() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val testRunnerLayout = PatternLayout()
        testRunnerLayout.context = loggerContext
        testRunnerLayout.pattern = "%-5level %-50.50logger{16} %msg%n"
        testRunnerLayout.start()
        testRunnerLogCollector = TestRunnerLogCollector(ServiceSpecification(name = TEST_RUNNER_NAME), testRunnerLayout)
        testRunnerLogCollector.context = loggerContext
        testRunnerLogCollector.name = "TestRunnerLogCollector"
        testRunnerLogCollector.start()
        val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        root.addAppender(testRunnerLogCollector)
    }
}
