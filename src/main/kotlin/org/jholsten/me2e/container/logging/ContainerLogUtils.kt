package org.jholsten.me2e.container.logging

import org.jholsten.me2e.container.logging.model.ContainerLogEntryList
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.output.FrameConsumerResultCallback
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.WaitingConsumer
import java.io.Closeable
import java.util.function.Consumer

/**
 * Utility class for managing Container logs.
 * Expands the functionality of [org.testcontainers.utility.LogUtils].
 * @see org.testcontainers.utility.LogUtils
 */
class ContainerLogUtils {
    companion object {
        /**
         * Returns all log output from the container [dockerContainer] from [since] until [until] along with their timestamps,
         * by executing `docker logs --since $since --until $until --timestamps $containerId`.
         *
         * To retrieve all log entries starting from the creation of the container, set [since] to `0`.
         * To retrieve all log entries until now, set [until] to `null`.
         * @param dockerContainer Docker Container for which logs are to be retrieved.
         * @param since Only return logs since this time, as a UNIX timestamp.
         * @param until Only return logs before this time, as a UNIX timestamp. If set to `null`, all log entries until now are returned.
         * @see org.testcontainers.utility.LogUtils.getOutput
         */
        @JvmStatic
        fun getLogs(dockerContainer: ContainerState, since: Int, until: Int?): ContainerLogEntryList {
            val collector = ContainerLogCollector()
            val wait = WaitingConsumer()
            val consumer = collector.andThen(wait)

            attachConsumer(dockerContainer, consumer, followStream = false, since = since, until = until).use {
                wait.waitUntilEnd()
                return collector.logs
            }
        }

        /**
         * Attaches the given [consumer] to the container's log output in follow mode.
         * The consumer will receive all previous and all future log frames.
         * @param dockerContainer Docker container to attach the log consumer to.
         * @param consumer Log consumer to be attached.
         * @return Consumer thread which can be closed to stop consuming log entries.
         * @see org.testcontainers.utility.LogUtils.followOutput
         */
        fun followOutput(dockerContainer: ContainerState, consumer: ContainerLogConsumer): Closeable {
            return attachConsumer(dockerContainer, consumer, followStream = true, since = 0)
        }

        private fun attachConsumer(
            dockerContainer: ContainerState,
            consumer: Consumer<OutputFrame>,
            followStream: Boolean,
            since: Int = 0,
            until: Int? = null,
        ): Closeable {
            val cmd = dockerContainer.dockerClient
                .logContainerCmd(dockerContainer.containerId)
                .withFollowStream(followStream)
                .withTimestamps(true)
                .withSince(since)
                .withStdOut(true)
                .withStdErr(true)

            if (until != null) {
                cmd.withUntil(until)
            }

            val callback = FrameConsumerResultCallback()
            callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer)
            callback.addConsumer(OutputFrame.OutputType.STDERR, consumer)

            return cmd.exec(callback)
        }
    }
}
