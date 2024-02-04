package org.jholsten.me2e.container.logging

import org.jholsten.me2e.container.logging.model.ContainerLogEntry
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.OutputFrame
import java.time.Instant
import java.util.function.Consumer

/**
 * Base class for consuming log entries of a container.
 * Whenever the container prints a log message, the callback function [accept] is executed.
 * @sample ContainerLogCollector
 */
abstract class ContainerLogConsumer : Consumer<OutputFrame> {
    private val logger = LoggerFactory.getLogger(ContainerLogConsumer::class.java)

    /**
     * Callback function to execute when a container logs a new entry. Empty messages are ignored
     * by the underlying consumer, so that this function is not called when a message does not
     * contain any non-whitespace characters.
     * When registering the consumer, all previous log entries are received.
     * @param entry Log entry received from the Docker container.
     */
    abstract fun accept(entry: ContainerLogEntry)

    override fun accept(t: OutputFrame) {
        try {
            parseLogEntry(t)?.let { accept(it) }
        } catch (e: Exception) {
            logger.error("Exception occurred while trying to consume log entry:", e)
        }
    }

    /**
     * Parses log entry for the given [frame]. Returns `null` for empty messages.
     * Reads timestamp from output or, if entry does not match the predefined Regex,
     * sets current time as the timestamp of the log entry. Since the format of the
     * messages is defined by Docker, it should never happen that a message does not
     * correspond to the predefined Regex [OUTPUT_REGEX].
     * @param frame Log entry frame to parse.
     */
    private fun parseLogEntry(frame: OutputFrame): ContainerLogEntry? {
        val output = frame.utf8String
        if (output.trim().isEmpty()) {
            return null
        }
        val match = OUTPUT_REGEX.find(output) ?: return ContainerLogEntry(Instant.now(), output)
        val (timestamp, message) = match.destructured
        if (message.trim().isEmpty()) {
            return null
        }
        return ContainerLogEntry(
            timestamp = Instant.parse(timestamp),
            message = message.trimEnd(),
        )
    }

    companion object {
        /**
         * Regex for matching an RFC 3339 timestamp.
         */
        private val TIMESTAMP_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:[Zz]|(?:[\\+|\\-](?:[01]\\d|2[0-3])))")

        /**
         * Regex for matching one [`docker logs`](https://docs.docker.com/engine/reference/commandline/logs/) output entry.
         * Includes the following groups:
         * - Group 1: RFC 3339 Timestamp
         * - Group 2: Message, which may span over multiple lines
         */
        private val OUTPUT_REGEX = Regex("^($TIMESTAMP_REGEX) ([\\S\\s]*)\$")
    }
}
