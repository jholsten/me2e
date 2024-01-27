@file:JvmSynthetic

package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.Layout
import org.jholsten.me2e.report.logs.model.AggregatedLogEntry
import org.jholsten.me2e.report.logs.model.ServiceSpecification
import java.time.Instant

/**
 * Collector which collects the messages logged by the Test Runner for one test execution.
 */
internal class TestRunnerLogCollector(
    /**
     * Representation of the test runner.
     */
    private val service: ServiceSpecification,

    /**
     * Layout to use for formatting the log messages of the test runner.
     */
    private val layout: Layout<ILoggingEvent>,
) : AppenderBase<ILoggingEvent>() {
    /**
     * List of log entries that were collected so far.
     */
    private val logs: MutableList<AggregatedLogEntry> = mutableListOf()

    override fun append(log: ILoggingEvent) {
        logs.add(
            AggregatedLogEntry(
                service = service,
                timestamp = Instant.ofEpochSecond(log.timeStamp),
                message = layout.doLayout(log),
            )
        )
    }

    /**
     * Resets the list of collected log entries.
     * Returns all entries that were collected so far.
     */
    internal fun reset(): List<AggregatedLogEntry> {
        val entries = logs.toList()
        logs.clear()
        return entries
    }
}
