package org.jholsten.me2e.container.logging

import java.time.Instant

/**
 * Model representing one log entry of a container.
 */
data class LogEntry(
    /**
     * Timestamp of when this entry was logged.
     * As the timestamp is recorded by the [LogConsumer], there may be minimal deviations
     * from the actual timestamp.
     */
    val timestamp: Instant,

    /**
     * Output that the container logged to STDERR or STDOUT.
     */
    val log: String,
) {
    override fun toString(): String {
        return "[$timestamp]\t$log"
    }
}
