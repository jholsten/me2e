@file:JvmSynthetic

package org.jholsten.me2e.report.logs

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.jholsten.me2e.report.logs.LogAggregator.Companion.TEST_RUNNER_NAME
import org.jholsten.me2e.report.logs.model.LogEntry

/**
 * Collector which collects the messages logged by the Test Runner for one test execution.
 */
internal class TestRunnerLogCollector : AppenderBase<ILoggingEvent>() {
    private val logs: MutableList<LogEntry> = mutableListOf()

    override fun append(log: ILoggingEvent) {
        logs.add(
            LogEntry(
                service = TEST_RUNNER_NAME,
                timestamp = log.instant,
                message = log.message,
            )
        )
    }

    /**
     * Resets the list of collected log entries.
     * Returns all entries that were collected so far.
     */
    internal fun reset(): List<LogEntry> {
        val entries = logs.toList()
        logs.clear()
        return entries
    }
}
