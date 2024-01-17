package org.jholsten.me2e.container.logging.model

import java.time.Instant

/**
 * Model representing one log entry.
 */
data class LogEntry(
    /**
     * Name of the service which logged this entry.
     */
    //val service: String, TODO

    /**
     * Timestamp of when this entry was logged.
     */
    val timestamp: Instant,

    /**
     * Message of the log entry.
     */
    val message: String,
)
