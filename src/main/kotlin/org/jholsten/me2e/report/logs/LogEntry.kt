package org.jholsten.me2e.report.logs

import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import java.time.Instant

/**
 * Model representing one log entry from one service.
 */
class LogEntry(
    /**
     * Name of the service which logged this entry.
     */
    val service: String,

    /**
     * Timestamp of when this entry was logged.
     */
    timestamp: Instant,

    /**
     * Message of the log entry.
     */
    message: String,
) : ContainerLogEntry(timestamp, message)
