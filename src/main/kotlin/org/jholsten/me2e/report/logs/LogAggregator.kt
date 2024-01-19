package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.read.ListAppender
import com.google.auto.service.AutoService
import io.github.netmikey.logunit.api.LogCapturer
import org.jholsten.me2e.container.Container
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory


/**
 * Service which collects all log entries from all containers.
 * After each test execution, the logs for this test are collected
 * from all containers and stored in [logs].
 * TODO: Add test runner logs
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

    lateinit var listAppender: AppenderBase<ILoggingEvent>


    class MyCustomAppender : AppenderBase<ILoggingEvent>() {
        val list: MutableList<ILoggingEvent> = mutableListOf()
        override fun append(event: ILoggingEvent) {
            list.add(event)
        }

        companion object {
            private val MY_MARKER = MarkerFactory.getMarker("MY_MARKER")
        }
    }

    @AutoService(TestExecutionListener::class)
    class LogListener : TestExecutionListener {
        override fun reportingEntryPublished(testIdentifier: TestIdentifier?, entry: ReportEntry?) {
            println(entry)
        }
    }


    /**
     * Initializes the collector by attaching log consumers to all containers.
     */
    init {
        for (container in containers) {
            container.addLogConsumer(consumers[container.name]!!)
        }
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        listAppender = MyCustomAppender();
        listAppender.context = loggerContext
        listAppender.name = "ABC"
        listAppender.start()
        val log = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        log.addAppender(listAppender)
        val l = LogCapturer.create()
        l.logProvider.
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
        for (container in containers) {
            logs.addAll(consumers[container.name]!!.reset())
        }
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
