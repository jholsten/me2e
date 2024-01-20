@file:JvmSynthetic

package org.jholsten.me2e.container.stats

import com.github.dockerjava.api.model.Statistics
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * A consumer for a container [Statistics] entry that enables to wait until the expected frame is received.
 * @see org.testcontainers.containers.output.WaitingConsumer
 */
internal class ContainerStatsWaitingConsumer : Consumer<Statistics> {
    private val entries: LinkedBlockingDeque<Statistics> = LinkedBlockingDeque()

    override fun accept(t: Statistics) {
        entries.add(t)
    }

    /**
     * Wait until Docker closes the stream of output.
     * @see org.testcontainers.containers.output.WaitingConsumer.waitUntilEnd
     */
    fun waitUntilEnd() {
        while (System.currentTimeMillis() < Long.MAX_VALUE) {
            try {
                val entry = entries.pollLast(100, TimeUnit.MILLISECONDS)

                if (entry != null) {
                    return
                }

                if (entries.isEmpty()) {
                    // sleep for a moment to avoid excessive CPU spinning
                    Thread.sleep(10L)
                }
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }
}
