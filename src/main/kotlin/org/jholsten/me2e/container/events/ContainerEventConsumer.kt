package org.jholsten.me2e.container.events

import com.github.dockerjava.api.model.Event
import org.jholsten.me2e.container.events.model.ContainerEvent
import org.jholsten.me2e.utils.logger
import java.time.Instant
import java.util.function.Consumer

/**
 * Base class for consuming events of a Docker container.
 * Whenever Docker sends a new event for the container, the callback function [accept] is executed.
 * @sample ContainerRestartListener
 */
abstract class ContainerEventConsumer : Consumer<Event> {
    private val logger = logger<ContainerEventConsumer>()

    /**
     * Callback function to execute when a new event is received for a container.
     * @param event Event received for the Docker container.
     */
    abstract fun accept(event: ContainerEvent)

    override fun accept(t: Event) {
        try {
            parseEvent(t)?.let { accept(it) }
        } catch (e: Exception) {
            logger.error("Exception occurred while trying to consume container event:", e)
        }
    }

    /**
     * Parses container event for the given [event]. Returns `null` in case any of the
     * required attributes is not set or if the event type could not be parsed.
     */
    private fun parseEvent(event: Event): ContainerEvent? {
        if (event.timeNano == null || event.action == null) {
            return null
        }
        val type = ContainerEvent.Type.fromString(event.action!!)
        if (type == null) {
            logger.warn("Unknown container event type ${event.action}. Event will be ignored.")
            return null
        }
        return ContainerEvent(
            timestamp = event.timeNano!!.toInstant(),
            type = type,
        )
    }

    /**
     * Parses value given in epoch nanoseconds to an instance of [Instant].
     */
    private fun Long.toInstant(): Instant {
        val seconds = this / 1_000_000_000L
        val nanos = this % 1_000_000_000L
        return Instant.ofEpochSecond(seconds, nanos)
    }
}
