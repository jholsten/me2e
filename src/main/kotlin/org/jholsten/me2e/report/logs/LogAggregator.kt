package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import org.jholsten.me2e.container.Container
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import org.jholsten.me2e.report.result.ReportDataAggregator
import org.jholsten.me2e.utils.logger
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service which collects all log entries from all containers and from the Test Runner.
 */
class LogAggregator internal constructor() {
    private val logger = logger(this)

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
     * Initializes the collector for consuming the Test Runner's logs when the test execution started.
     * Attaches consumer to the root logger to start capturing its logs.
     */
    @JvmSynthetic
    internal fun initializeOnTestExecutionStarted() {
        consumeTestRunnerLogs()
        logger.info("Attached log consumer to Test Runner logs.")
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
        logger.info("Attached log consumer to container ${container.name}.")
    }

    /**
     * Callback function to execute when the execution of all tests has finished.
     * Collects logs from the test runner.
     * @return Collected log entries.
     */
    @JvmSynthetic
    internal fun collectTestRunnerLogs(): List<AggregatedLogEntry> {
        return testRunnerLogCollector.collect().toList()
    }

    /**
     * Callback function to execute when the execution of all tests has finished.
     * Collects logs from all containers.
     * @return Collected log entries.
     */
    @JvmSynthetic
    internal fun collectContainerLogs(): List<AggregatedLogEntry> {
        val logs = mutableListOf<AggregatedLogEntry>()
        for (consumer in consumers.values) {
            logs.addAll(consumer.collect())
        }
        logs.sortBy { it.timestamp }
        return logs.toList()
    }

    private fun consumeTestRunnerLogs() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val testRunnerLayout = PatternLayout()
        testRunnerLayout.context = loggerContext
        testRunnerLayout.pattern = "%-5level %-50.50logger{16} %msg%n"
        testRunnerLayout.start()
        testRunnerLogCollector = TestRunnerLogCollector(ReportDataAggregator.testRunner, testRunnerLayout)
        testRunnerLogCollector.context = loggerContext
        testRunnerLogCollector.name = "TestRunnerLogCollector"
        testRunnerLogCollector.start()
        val root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
        root.addAppender(testRunnerLogCollector)
    }
}
