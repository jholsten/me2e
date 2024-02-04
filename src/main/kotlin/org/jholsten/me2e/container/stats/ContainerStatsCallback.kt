package org.jholsten.me2e.container.stats

import com.github.dockerjava.api.async.ResultCallbackTemplate
import com.github.dockerjava.api.model.Statistics
import java.util.function.Consumer

/**
 * Callback for the consumption of statistics entries of a Docker container.
 */
internal class ContainerStatsCallback : ResultCallbackTemplate<ContainerStatsCallback, Statistics>() {
    /**
     * Registered consumers of this result callback.
     */
    private val consumers: MutableList<Consumer<Statistics>> = mutableListOf()

    /**
     * Registers the given consumer to be notified when a new statistics entry is received.
     */
    @JvmSynthetic
    fun addConsumer(consumer: Consumer<Statistics>) {
        consumers.add(consumer)
    }

    /**
     * Callback function which is executed when a new statistics entry is received.
     * Notifies all registered consumers.
     */
    @JvmSynthetic
    override fun onNext(obj: Statistics?) {
        if (obj != null) {
            consumers.forEach { it.accept(obj) }
        }
    }
}
