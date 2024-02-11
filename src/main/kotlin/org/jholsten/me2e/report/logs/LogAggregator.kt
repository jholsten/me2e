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
internal class LogAggregator {
    private val logger = logger<LogAggregator>()

    /**
     * Map of container specification and corresponding log collector.
     * For each container instance, there is one collector which captures the container's logs.
     */
    private val containerLogCollectors: MutableMap<ServiceSpecification, ContainerLogCollector> = mutableMapOf()

    /**
     * Consumer of the Test Runner's logs.
     * Records all log entries which are output to any SLF4J logger.
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
     * @param container Container which has been started and whose logs should be captured.
     * @param specification Representation of the [container] instance.
     */
    @JvmSynthetic
    internal fun onContainerStarted(container: Container, specification: ServiceSpecification) {
        val consumer = ContainerLogCollector(specification)
        this.containerLogCollectors[specification] = consumer
        container.addLogConsumer(consumer)
        logger.info("Attached log consumer to container ${container.name}.")
    }

    /**
     * Callback function to execute when the execution of one test has finished. Collects logs from the Test Runner.
     * When invoking this method, the log entries previously collected in the collector are reset so that the logs
     * can be distinctly assigned to the tests.
     * @return Collected log entries from the Test Runner.
     */
    @JvmSynthetic
    internal fun collectTestRunnerLogs(): List<AggregatedLogEntry> {
        return testRunnerLogCollector.collect().toList()
    }

    /**
     * Callback function to execute when the execution of all tests has finished. Collects logs from all containers.
     * Unlike the collector of the Test Runner's logs, there may be delays in the arrival of the log entries with the
     * containers. If a container is currently busy, its logs may not be consumed until later which would lead to the
     * logs being assigned to the wrong test. Therefore, the logs of the containers are only collected after all tests
     * have been executed and assigned to the corresponding tests via their timestamps.
     * @return Collected log entries from all containers.
     */
    @JvmSynthetic
    internal fun collectContainerLogs(): List<AggregatedLogEntry> {
        val logs = mutableListOf<AggregatedLogEntry>()
        for (consumer in containerLogCollectors.values) {
            logs.addAll(consumer.collect())
        }
        logs.sortBy { it.timestamp }
        return logs.toList()
    }

    /**
     * Attaches log consumer [testRunnerLogCollector] to the Test Runner's root logger.
     * Formats each captured log message using a predefined format.
     */
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
