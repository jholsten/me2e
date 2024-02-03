package org.jholsten.me2e.events

import com.github.dockerjava.api.model.Event
import org.jholsten.me2e.events.model.ContainerEvent
import org.testcontainers.containers.ContainerState
import java.io.Closeable
import java.util.function.Consumer

/**
 * Utility class for reading events of a Docker container.
 */
internal class ContainerEventsUtils {
    companion object {
        /**
         * Attaches the given [consumer] to the container's events, by executing `docker events --filter container=$containerId`.
         * Docker instantiates a live data stream for the container and for each event received by Docker, the consumer is notified.
         * @param dockerContainer Docker container to attach the consumer to.
         * @param consumer Event consumer to be attached.
         * @param eventFilters If provided, only events of the given types are consumed.
         * @return Consumer thread which can be closed to stop consuming events.
         */
        fun followOutput(
            dockerContainer: ContainerState,
            consumer: ContainerEventConsumer,
            eventFilters: List<ContainerEvent.Type>?,
        ): Closeable {
            return attachConsumer(dockerContainer, consumer, eventFilters)
        }

        private fun attachConsumer(
            dockerContainer: ContainerState,
            consumer: Consumer<Event>,
            eventFilters: List<ContainerEvent.Type>?,
        ): Closeable {
            val cmd = dockerContainer.dockerClient
                .eventsCmd()
                .withContainerFilter(dockerContainer.containerId)

            if (eventFilters != null) {
                cmd.withEventFilter(*eventFilters.map { it.value }.toTypedArray())
            }

            val callback = ContainerEventCallback()
            callback.addConsumer(consumer)

            return cmd.exec(callback)
        }
    }
}
