package org.jholsten.me2e.container.logging

import org.testcontainers.containers.output.OutputFrame
import java.time.Instant
import java.util.function.Consumer

/**
 * Consumer which receives all of a container's log outputs.
 * Stores outputs with timestamp in local variable.
 */
class LogConsumer : Consumer<OutputFrame> {
    /**
     * List of log entries that this consumer collected so far.
     */
    val logs: LogEntryList = LogEntryList()

    /**
     * Callback function which is executed when the container outputs a new frame to STDOUT or STDERR.
     * Stores frame in local variable.
     */
    override fun accept(frame: OutputFrame) {
        logs.add(LogEntry(Instant.now(), frame.utf8String))
    }
}
