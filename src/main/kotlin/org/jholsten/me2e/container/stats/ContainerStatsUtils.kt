@file:JvmSynthetic

package org.jholsten.me2e.container.stats

import com.github.dockerjava.api.model.Statistics
import org.jholsten.me2e.container.stats.model.ContainerStatsEntry
import org.testcontainers.containers.ContainerState
import java.io.Closeable
import java.util.function.Consumer

/**
 * Utility class for reading statistics of a Docker container.
 */
internal class ContainerStatsUtils {
    companion object {
        /**
         * Returns the current resource usage statistics of the given Docker container,
         * by executing `docker stats --no-stream $containerId`.
         * @param dockerContainer Docker Container for which statistics are to be retrieved.
         */
        fun getStats(dockerContainer: ContainerState): ContainerStatsEntry {
            val collector = ContainerStatsCollector()
            val wait = ContainerStatsWaitingConsumer()
            val consumer = collector.andThen(wait)

            attachConsumer(dockerContainer, consumer, followStream = false).use {
                wait.waitUntilEnd()
                return collector.stats.first()
            }
        }

        /**
         * Attaches the given [consumer] to the container's resource usage statistics,
         * by executing `docker stats $containerId`.
         * Docker instantiates a live data stream for the container and for each statistics
         * entry received by Docker, the consumer is notified.
         * @param dockerContainer Docker container to attach the consumer to.
         * @param consumer Statistics consumer to be attached.
         * @return Consumer thread which can be closed to stop consuming statistics entries.
         */
        fun followOutput(dockerContainer: ContainerState, consumer: ContainerStatsConsumer): Closeable {
            return attachConsumer(dockerContainer, consumer, followStream = true)
        }

        private fun attachConsumer(dockerContainer: ContainerState, consumer: Consumer<Statistics>, followStream: Boolean): Closeable {
            val cmd = dockerContainer.dockerClient
                .statsCmd(dockerContainer.containerId)
                .withNoStream(!followStream)

            val callback = ContainerStatsCallback()
            callback.addConsumer(consumer)

            return cmd.exec(callback)
        }
    }
}
