package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.Layout
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification

/**
 * Collector which collects the messages logged by the Test Runner for one test execution.
 */
internal class TestRunnerLogCollector(
    /**
     * Representation of the Test Runner.
     */
    private val service: ServiceSpecification,

    /**
     * Layout to use for formatting the log messages of the Test Runner.
     */
    private val layout: Layout<ILoggingEvent>,
) : AppenderBase<ILoggingEvent>() {
    /**
     * List of log entries that were collected so far.
     */
    private val logs: MutableList<AggregatedLogEntry> = mutableListOf()

    /**
     * Callback function which is executed when the Test Runner logs a new entry.
     * Adds the entry to the list of collected [logs].
     */
    @JvmSynthetic
    override fun append(log: ILoggingEvent) {
        logs.add(
            AggregatedLogEntry(
                service = service,
                timestamp = log.instant,
                message = layout.doLayout(log),
            )
        )
    }

    /**
     * Returns all log entries that were collected so far. Resets the list of collected entries
     * afterwards to be able to distinctly assign logs to the corresponding tests.
     * @return Log entries collected so far from the Test Runner.
     */
    @JvmSynthetic
    fun collect(): List<AggregatedLogEntry> {
        val entries = logs.toList()
        logs.clear()
        return entries
    }
}
