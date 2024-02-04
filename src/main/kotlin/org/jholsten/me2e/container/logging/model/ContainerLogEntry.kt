package org.jholsten.me2e.container.logging.model

import java.time.Instant

/**
 * Model representing one log entry of a container returned by `docker logs`.
 * @see org.testcontainers.containers.output.OutputFrame
 */
open class ContainerLogEntry(
    /**
     * Timestamp of when this entry was logged.
     */
    val timestamp: Instant,

    /**
     * Message of the log entry.
     */
    val message: String,
) {
    override fun toString(): String {
        return "[$timestamp]\t$message"
    }
}
